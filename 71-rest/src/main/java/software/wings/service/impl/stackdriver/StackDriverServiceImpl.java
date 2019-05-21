package software.wings.service.impl.stackdriver;

import static io.harness.eraro.ErrorCode.STACKDRIVER_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.VerificationConstants.STACK_DRIVER_METRIC;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.DelegateTask;
import io.harness.exception.WingsException;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.service.intfc.stackdriver.StackDriverService;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by Pranjal on 11/27/2018
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class StackDriverServiceImpl implements StackDriverService {
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
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(StackDriverSetupTestNodeData setupTestNodeData) {
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
                                            .timeout(DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                            .build();
      return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
          .getMetricsWithDataForNode((GcpConfig) settingAttribute.getValue(), encryptionDetails, setupTestNodeData,
              hostName,
              createApiCallLog(
                  settingAttribute.getAccountId(), setupTestNodeData.getAppId(), setupTestNodeData.getGuid()));
    } catch (Exception e) {
      logger.info("error getting metric data for node", e);
      throw new WingsException(STACKDRIVER_ERROR)
          .addParam("message", "Error in getting metric data for the node. " + e.getMessage());
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
      throw new WingsException("AWS account setting not found " + settingId);
    }
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();
    return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
        .listRegions((GcpConfig) settingAttribute.getValue(), encryptionDetails);
  }

  @Override
  public Map<String, String> listForwardingRules(String settingId, String region) throws IOException {
    SettingAttribute settingAttribute = settingsService.get(settingId);

    if (settingAttribute == null || !(settingAttribute.getValue() instanceof GcpConfig)) {
      throw new WingsException("AWS account setting not found " + settingId);
    }
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT * 3)
                                          .build();
    return delegateProxyFactory.get(StackDriverDelegateService.class, syncTaskContext)
        .listForwardingRules((GcpConfig) settingAttribute.getValue(), encryptionDetails, region);
  }

  private static Map<String, List<StackDriverMetric>> fetchMetrics() {
    Map<String, List<StackDriverMetric>> stackDriverMetrics;
    YamlUtils yamlUtils = new YamlUtils();
    try {
      URL url = CloudWatchService.class.getResource(STACK_DRIVER_METRIC);
      String yaml = Resources.toString(url, Charsets.UTF_8);
      stackDriverMetrics = yamlUtils.read(yaml, new TypeReference<Map<String, List<StackDriverMetric>>>() {});
    } catch (Exception e) {
      throw new WingsException(e);
    }
    return stackDriverMetrics;
  }
}
