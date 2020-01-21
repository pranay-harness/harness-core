package software.wings.service.impl.instana;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.VerificationConstants.INSTANA_DOCKER_PLUGIN;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDERV2;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InstanaConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.instana.InstanaDelegateService;
import software.wings.service.intfc.instana.InstanaService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class InstanaServiceImpl implements InstanaService {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(InstanaSetupTestNodeData setupTestNodeData) {
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(settingAttribute.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();
    InstanaConfig instanaConfig = (InstanaConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(instanaConfig);

    InstanaInfraMetricRequest infraMetricRequest =
        InstanaInfraMetricRequest.builder()
            .timeframe(InstanaTimeFrame.builder()
                           .windowSize(TimeUnit.MINUTES.toMillis(15))
                           .to(Timestamp.currentMinuteBoundary())
                           .build())
            .metrics(setupTestNodeData.getMetrics())
            .plugin(INSTANA_DOCKER_PLUGIN)
            .rollup(60)
            .query(setupTestNodeData.getQuery().replace(
                VERIFICATION_HOST_PLACEHOLDERV2, "\"" + setupTestNodeData.getInstanceName() + "\""))
            .build();
    VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
        VerificationNodeDataSetupResponse.builder().build();
    VerificationNodeDataSetupResponse.VerificationLoadResponse verificationLoadResponse =
        VerificationNodeDataSetupResponse.VerificationLoadResponse.builder().isLoadPresent(false).build();
    InstanaDelegateService instanaDelegateService =
        delegateProxyFactory.get(InstanaDelegateService.class, syncTaskContext);
    try {
      ThirdPartyApiCallLog apiCallLog = createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid());
      InstanaInfraMetrics infraMetrics =
          instanaDelegateService.getInfraMetrics(instanaConfig, encryptionDetails, infraMetricRequest, apiCallLog);
      verificationNodeDataSetupResponse.setProviderReachable(true);
      boolean isLoadPresent =
          !infraMetrics.getItems().isEmpty() && !infraMetrics.getItems().get(0).getMetrics().isEmpty();
      verificationLoadResponse.setLoadPresent(isLoadPresent);
      if (isLoadPresent) {
        verificationLoadResponse.setLoadResponse(infraMetrics);
        verificationNodeDataSetupResponse.setDataForNode(infraMetrics);
      }
      verificationNodeDataSetupResponse.setLoadResponse(verificationLoadResponse);
    } catch (DataCollectionException e) {
      verificationNodeDataSetupResponse.setProviderReachable(false);
    }

    return verificationNodeDataSetupResponse;
  }
}
