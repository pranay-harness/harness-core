package software.wings.verification;

import com.google.inject.Inject;

import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration.CVConfigurationYaml;

public abstract class CVConfigurationYamlHandler<Y extends CVConfigurationYaml, B extends CVConfiguration>
    extends BaseYamlHandler<Y, B> {
  @Inject YamlHelper yamlHelper;
  @Inject CVConfigurationService cvConfigurationService;

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {}

  public void toYaml(CVConfigurationYaml yaml, CVConfiguration bean) {
    yaml.setAccountId(bean.getAccountId());
    yaml.setAnalysisTolerance(bean.getAnalysisTolerance());
    yaml.setConnectorId(bean.getConnectorId());
    yaml.setEnvId(bean.getEnvId());
    yaml.setName(bean.getName());
    yaml.setServiceId(bean.getServiceId());
    yaml.setStateType(bean.getStateType());
  }

  public void toBean(ChangeContext<Y> changeContext, B bean, String appId, String yamlPath) {
    Y yaml = changeContext.getYaml();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setAppId(appId);
    bean.setName(name);

    bean.setAccountId(yaml.getAccountId());
    bean.setEnvId(yaml.getEnvId());
    bean.setEnabled24x7(yaml.isEnabled24x7());
    bean.setStateType(yaml.getStateType());
    bean.setAnalysisTolerance(yaml.getAnalysisTolerance());
    bean.setServiceId(yaml.getServiceId());
    bean.setConnectorId(yaml.getConnectorId());
  }
}
