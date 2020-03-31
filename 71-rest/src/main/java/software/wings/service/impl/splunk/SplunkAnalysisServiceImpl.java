package software.wings.service.impl.splunk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtils;
import software.wings.service.intfc.splunk.SplunkAnalysisService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.StateType;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pranjal on 08/31/2018
 */
@Singleton
@Slf4j
public class SplunkAnalysisServiceImpl extends AnalysisServiceImpl implements SplunkAnalysisService {
  @Inject private MLServiceUtils mlServiceUtils;
  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(
      String accountId, SplunkSetupTestNodeData setupTestNodeData) {
    long startTime = TimeUnit.SECONDS.toMillis(setupTestNodeData.getFromTime());
    long endTime = TimeUnit.SECONDS.toMillis(setupTestNodeData.getToTime());
    logger.info("Starting Log Data collection by Host for account Id : {}, SplunkSetupTestNodeData : {}", accountId,
        setupTestNodeData);

    // gets the settings attributes for given settings id
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    logger.info("Settings attribute : " + settingAttribute);
    if (settingAttribute == null) {
      throw new WingsException(
          "No " + StateType.SPLUNKV2 + " setting with id: " + setupTestNodeData.getSettingId() + " found");
    }
    ThirdPartyApiCallLog apiCallLog = createApiCallLog(settingAttribute.getAccountId(), setupTestNodeData.getGuid());

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext taskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    List<LogElement> responseWithoutHost =
        delegateProxyFactory.get(SplunkDelegateService.class, taskContext)
            .getLogResults((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails,
                setupTestNodeData.getQuery(), setupTestNodeData.getHostNameField(), null, startTime, endTime,
                apiCallLog, 0, setupTestNodeData.isAdvancedQuery());
    if (isEmpty(responseWithoutHost)) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    }

    if (setupTestNodeData.isServiceLevel()) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder()
                            .isLoadPresent(!responseWithoutHost.isEmpty())
                            .loadResponse(responseWithoutHost)
                            .build())
          .build();
    }

    String hostName = mlServiceUtils.getHostName(setupTestNodeData);
    List<LogElement> responseWithHost =
        delegateProxyFactory.get(SplunkDelegateService.class, taskContext)
            .getLogResults((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails,
                setupTestNodeData.getQuery(), setupTestNodeData.getHostNameField(), hostName, startTime, endTime,
                apiCallLog, 0, setupTestNodeData.isAdvancedQuery());

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder().loadResponse(responseWithoutHost).isLoadPresent(true).build())
        .dataForNode(responseWithHost)
        .build();
  }
}
