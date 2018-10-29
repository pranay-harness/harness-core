package software.wings.service.impl.splunk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.MLServiceUtil;
import software.wings.service.intfc.splunk.SplunkAnalysisService;
import software.wings.service.intfc.splunk.SplunkDelegateService;
import software.wings.sm.StateType;

import java.util.List;

/**
 * Created by Pranjal on 08/31/2018
 */
public class SplunkAnalysisServiceImpl extends AnalysisServiceImpl implements SplunkAnalysisService {
  private static final Logger logger = LoggerFactory.getLogger(SplunkAnalysisServiceImpl.class);

  @Inject private MLServiceUtil mlServiceUtil;
  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(
      String accountId, SplunkSetupTestNodeData setupTestNodeData) {
    logger.info("Starting Log Data collection by Host for account Id : {}, SplunkSetupTestNodeData : {}", accountId,
        setupTestNodeData);

    // gets the settings attributes for given settings id
    final SettingAttribute settingAttribute = settingsService.get(setupTestNodeData.getSettingId());
    logger.info("Settings attribute : " + settingAttribute);
    if (settingAttribute == null) {
      throw new WingsException(
          "No " + StateType.SPLUNKV2 + " setting with id: " + setupTestNodeData.getSettingId() + " found");
    }
    ThirdPartyApiCallLog apiCallLog = apiCallLogWithDummyStateExecution(accountId);

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext taskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    List<LogElement> responseWithoutHost =
        delegateProxyFactory.get(SplunkDelegateService.class, taskContext)
            .getLogResults((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails,
                setupTestNodeData.getQuery(), setupTestNodeData.getHostNameField(), null,
                setupTestNodeData.getFromTime(), setupTestNodeData.getToTime(), apiCallLog, 0);
    if (isEmpty(responseWithoutHost)) {
      return VerificationNodeDataSetupResponse.builder()
          .providerReachable(true)
          .loadResponse(VerificationLoadResponse.builder().isLoadPresent(false).build())
          .build();
    }

    String hostName = mlServiceUtil.getHostNameFromExpression(setupTestNodeData);
    List<LogElement> responseWithHost =
        delegateProxyFactory.get(SplunkDelegateService.class, taskContext)
            .getLogResults((SplunkConfig) settingAttribute.getValue(), encryptedDataDetails,
                setupTestNodeData.getQuery(), setupTestNodeData.getHostNameField(), hostName,
                setupTestNodeData.getFromTime(), setupTestNodeData.getToTime(), apiCallLog, 0);

    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(true)
        .loadResponse(VerificationLoadResponse.builder().loadResponse(responseWithoutHost).isLoadPresent(true).build())
        .dataForNode(responseWithHost)
        .build();
  }
}
