package software.wings.service.impl.sumo;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;

import java.util.List;

/**
 * Created by Pranjal on 08/23/2018
 */
@Singleton
public class SumoLogicAnalysisServiceImpl extends AnalysisServiceImpl implements SumoLogicAnalysisService {
  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(
      String accountId, SumoLogicSetupTestNodedata sumoLogicSetupTestNodedata) {
    final SettingAttribute settingAttribute = settingsService.get(sumoLogicSetupTestNodedata.getSettingId());
    if (settingAttribute == null) {
      throw new WingsException("No setting with id: " + sumoLogicSetupTestNodedata.getSettingId() + " found");
    }
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);
    SyncTaskContext sumoTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(SumoDelegateService.class, sumoTaskContext)
        .getLogDataByHost(accountId, (SumoConfig) settingAttribute.getValue(), sumoLogicSetupTestNodedata.getQuery(),
            sumoLogicSetupTestNodedata.getHostNameField(),
            sumoLogicSetupTestNodedata.isServiceLevel() ? null
                                                        : sumoLogicSetupTestNodedata.getInstanceElement().getHostName(),
            encryptedDataDetails,
            createApiCallLog(settingAttribute.getAccountId(), sumoLogicSetupTestNodedata.getAppId(),
                sumoLogicSetupTestNodedata.getGuid()));
  }
}
