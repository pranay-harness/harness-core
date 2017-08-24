package software.wings.service.impl.elk;

import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.elk.ElkAnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;

import java.io.IOException;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by rsingh on 8/23/17.
 */
public class ElkAnalysisServiceImpl extends AnalysisServiceImpl implements ElkAnalysisService {
  @Inject protected SettingsService settingsService;
  @Inject protected DelegateProxyFactory delegateProxyFactory;

  @Override
  public Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No elk setting with id: " + analysisServerConfigId + " found");
    }

    final ElkConfig elkConfig = (ElkConfig) settingAttribute.getValue();
    SyncTaskContext elkTaskContext =
        aContext().withAccountId(settingAttribute.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();
    return delegateProxyFactory.get(ElkDelegateService.class, elkTaskContext).getIndices(elkConfig);
  }
}
