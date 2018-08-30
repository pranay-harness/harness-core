package software.wings.service.impl.sumo;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import software.wings.annotation.Encryptable;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.sumo.SumoDelegateService;
import software.wings.service.intfc.sumo.SumoLogicAnalysisService;

import java.util.List;

/**
 * Created by Pranjal on 08/23/2018
 */
public class SumoLogicAnalysisServiceImpl extends AnalysisServiceImpl implements SumoLogicAnalysisService {
  @Override
  public VerificationNodeDataSetupResponse getLogDataByHost(
      String accountId, SumoLogicSetupTestNodedata sumoLogicSetupTestNodedata) {
    final SettingAttribute settingAttribute = settingsService.get(sumoLogicSetupTestNodedata.getSettingId());
    if (settingAttribute == null) {
      throw new WingsException("No setting with id: " + sumoLogicSetupTestNodedata.getSettingId() + " found");
    }
    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);
    SyncTaskContext sumoTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(SumoDelegateService.class, sumoTaskContext)
        .getLogDataByHost(accountId, (SumoConfig) settingAttribute.getValue(), sumoLogicSetupTestNodedata.getQuery(),
            sumoLogicSetupTestNodedata.getHostNameField(),
            sumoLogicSetupTestNodedata.getInstanceElement().getHostName(), encryptedDataDetails);
  }
}
