package software.wings.service.impl.elk;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.annotation.Encryptable;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 8/23/17.
 */
@SuppressFBWarnings("MF_CLASS_MASKS_FIELD")
public class ElkAnalysisServiceImpl extends AnalysisServiceImpl implements ElkAnalysisService {
  @Inject protected SettingsService settingsService;
  @Inject protected DelegateProxyFactory delegateProxyFactory;

  @Override
  public Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails((Encryptable) settingAttribute.getValue(), null, null);

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    SyncTaskContext elkTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
        .getIndices(elkConfig, encryptedDataDetails, null);
  }

  @Override
  public String getVersion(String accountId, ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException {
    SyncTaskContext elkTaskContext = aContext().withAccountId(accountId).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext)
        .getVersion(elkConfig, encryptedDataDetails);
  }
}
