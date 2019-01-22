package software.wings.service.impl.stackdriver;

import static io.harness.eraro.ErrorCode.STACKDRIVER_ERROR;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.exception.WingsException;
import io.harness.serializer.YamlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.common.Constants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.stackdriver.StackDriverDelegateService;
import software.wings.service.intfc.stackdriver.StackDriverService;

import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by Pranjal on 11/27/2018
 */
@ValidateOnExecution
@Singleton
public class StackDriverServiceImpl implements StackDriverService {
  private static final Logger logger = LoggerFactory.getLogger(StackDriverServiceImpl.class);

  @Inject private SettingsService settingsService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private MLServiceUtil mlServiceUtil;
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
      hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    }

    try {
      final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
      List<EncryptedDataDetail> encryptionDetails =
          secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
      SyncTaskContext syncTaskContext = aContext()
                                            .withAccountId(settingAttribute.getAccountId())
                                            .withAppId(Base.GLOBAL_APP_ID)
                                            .withTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 3)
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

  private static Map<String, List<StackDriverMetric>> fetchMetrics() {
    Map<String, List<StackDriverMetric>> stackDriverMetrics;
    YamlUtils yamlUtils = new YamlUtils();
    try {
      URL url = CloudWatchService.class.getResource(Constants.STACK_DRIVER_METRIC);
      String yaml = Resources.toString(url, Charsets.UTF_8);
      stackDriverMetrics = yamlUtils.read(yaml, new TypeReference<Map<String, List<StackDriverMetric>>>() {});
    } catch (Exception e) {
      throw new WingsException(e);
    }
    return stackDriverMetrics;
  }
}
