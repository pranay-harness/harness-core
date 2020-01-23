package software.wings.service.impl.stackdriver;

import static io.harness.eraro.ErrorCode.STACKDRIVER_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.VerificationConstants.STACK_DRIVER_METRIC;
import static software.wings.common.VerificationConstants.TIME_DURATION_FOR_LOGS_IN_MINUTES;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.sm.StateType;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by Pranjal on 11/27/2018
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class StackDriverServiceImpl implements StackDriverService {
  private static final URL STACKDRIVER_METRICS_URL = StackDriverService.class.getResource(STACK_DRIVER_METRIC);
  private static final String STACKDRIVER_YAML;
  static {
    String tmpStackDriverYaml = "";
    try {
      tmpStackDriverYaml = Resources.toString(STACKDRIVER_METRICS_URL, Charsets.UTF_8);
    } catch (IOException ex) {
      logger.info("Exception while reading StackDriver yaml", ex);
    }
    STACKDRIVER_YAML = tmpStackDriverYaml;
  }
  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtils mlServiceUtils;
  @Inject private EncryptionService encryptionService;

  private final Map<String, List<StackDriverMetric>> metricsByNameSpace;

  @Inject
  public StackDriverServiceImpl() {
    metricsByNameSpace = fetchMetrics();
  }

  @Override
  public VerificationNodeDataSetupResponse getDataForNode(StackDriverSetupTestNodeData setupTestNodeData) {
    String hostName = null;
    // check if it is for service level, serviceId is empty then get hostname
    if (!setupTestNodeData.isServiceLevel()) {
      hostName = mlServiceUtils.getHostNameFromExpression(setupTestNodeData);
    }

    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();

      if (StateType.STACK_DRIVER_LOG == setupTestNodeData.getStateType()) {
        return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
            .getLogWithDataForNode(setupTestNodeData.getGuid(), (GcpConfig) settingAttribute.getValue(),
                encryptionDetails, hostName, setupTestNodeData);
      } else {
        return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
            .getMetricsWithDataForNode((GcpConfig) settingAttribute.getValue(), encryptionDetails, setupTestNodeData,
                hostName, createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid()));
      }
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(STACKDRIVER_ERROR)
          .addParam("reason", "Error in getting metric data for the node. " + e.getMessage());
    }
  }

  @Override
  public Object getLogSample(String accountId, String serverConfigId, String query, String guid) {
    try {
      final SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                            .accountId(settingAttribute.getAccountId())
                                            .appId(GLOBAL_APP_ID)
                                            .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();

      return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
          .getLogSample(guid, (GcpConfig) settingAttribute.getValue(), encryptionDetails, query,
              System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1), System.currentTimeMillis());
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(STACKDRIVER_ERROR)
          .addParam("reason", "Error in getting sample log data." + e.getMessage());
    }
  }

  @Override
  public Map<String, List<StackDriverMetric>> getMetrics() {
    return metricsByNameSpace;
  }

  @Override
  public List<String> listRegions(String settingId) throws IOException {
    SettingAttribute settingAttribute = settingsService.get(settingId);

    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GcpConfig)) {
      throw new WingsException("GCP account setting not found " + settingId);
    }
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();
    return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
        .listRegions((GcpConfig) settingAttribute.getValue(), encryptionDetails);
  }

  @Override
  public Map<String, String> listForwardingRules(String settingId, String region) throws IOException {
    SettingAttribute settingAttribute = settingsService.get(settingId);

    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GcpConfig)) {
      throw new WingsException("GCP account setting not found " + settingId);
    }
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();
    return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
        .listForwardingRules((GcpConfig) settingAttribute.getValue(), encryptionDetails, region);
  }

  @Override
  public Boolean validateQuery(
      String accountId, String appId, String connectorId, String query, String hostNameField, String logMessageField) {
    SettingAttribute settingAttribute = settingsService.get(connectorId);

    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GcpConfig)) {
      throw new WingsException("GCP account setting not found " + connectorId);
    }
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(accountId)
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(TaskData.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();

    StackDriverSetupTestNodeData stackDriverSetupTestNodeData =
        StackDriverSetupTestNodeData.builder()
            .query(query)
            .fromTime(OffsetDateTime.now().minusMinutes(TIME_DURATION_FOR_LOGS_IN_MINUTES + 2).toEpochSecond())
            .toTime(OffsetDateTime.now().minusMinutes(2).toEpochSecond())
            .hostnameField(hostNameField)
            .messageField(logMessageField)
            .isServiceLevel(true)
            .build();
    VerificationNodeDataSetupResponse nodeDataSetupResponse =
        delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
            .getLogWithDataForNode(
                null, (GcpConfig) settingAttribute.getValue(), encryptionDetails, null, stackDriverSetupTestNodeData);
    List<LogElement> response = nodeDataSetupResponse.getDataForNode() != null
        ? (List<LogElement>) nodeDataSetupResponse.getDataForNode()
        : Collections.emptyList();

    if (response.size() / TIME_DURATION_FOR_LOGS_IN_MINUTES > VerificationConstants.TOTAL_HITS_PER_MIN_THRESHOLD) {
      throw new WingsException(ErrorCode.STACKDRIVER_ERROR, "Too many logs to process, please refine your query")
          .addParam("reason", "Too many logs returned using query: '" + query + "'. Please refine your query.");
    }
    return true;
  }

  private static Map<String, List<StackDriverMetric>> fetchMetrics() {
    Map<String, List<StackDriverMetric>> stackDriverMetrics;
    YamlUtils yamlUtils = new YamlUtils();
    try {
      stackDriverMetrics =
          yamlUtils.read(STACKDRIVER_YAML, new TypeReference<Map<String, List<StackDriverMetric>>>() {});
    } catch (Exception e) {
      throw new WingsException(e);
    }
    return stackDriverMetrics;
  }
}
