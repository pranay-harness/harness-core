package software.wings.verification;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration.CVConfigurationYaml;

import java.util.Date;

public abstract class CVConfigurationYamlHandler<Y extends CVConfigurationYaml, B extends CVConfiguration>
    extends BaseYamlHandler<Y, B> {
  @Inject YamlHelper yamlHelper;
  @Inject CVConfigurationService cvConfigurationService;
  @Inject SettingsService settingsService;
  @Inject AppService appService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public void delete(ChangeContext<Y> changeContext) {}

  public void toYaml(CVConfigurationYaml yaml, CVConfiguration bean) {
    yaml.setAccountId(bean.getAccountId());
    yaml.setAnalysisTolerance(bean.getAnalysisTolerance());
    yaml.setConnectorName(settingsService.get(bean.getConnectorId()).getName());
    yaml.setName(bean.getName());
    yaml.setEnabled24x7(bean.isEnabled24x7());

    Application application = appService.get(bean.getAppId());
    Environment environment = environmentService.get(application.getUuid(), bean.getEnvId());
    Service service = serviceResourceService.get(application.getUuid(), bean.getServiceId());

    yaml.setHarnessApplicationName(application.getName());
    yaml.setEnvName(environment.getName());
    yaml.setServiceName(service.getName());
    yaml.setAlertThreshold(bean.getAlertThreshold());
    if (bean.getSnoozeStartTime() > 0) {
      yaml.setSnoozeStartTime(new Date(bean.getSnoozeStartTime()));
    }

    if (bean.getSnoozeEndTime() > 0) {
      yaml.setSnoozeEndTime(new Date(bean.getSnoozeEndTime()));
    }
  }

  public void toBean(ChangeContext<Y> changeContext, B bean, String appId, String yamlPath) {
    Y yaml = changeContext.getYaml();
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    bean.setAppId(appId);
    bean.setName(name);

    Application harnessApp = appService.getAppByName(yaml.getAccountId(), yaml.getHarnessApplicationName());
    if (harnessApp == null) {
      throw new WingsException("Invalid Harness ApplicationName provided in Yaml for CVConfiguration.");
    }
    Environment environment = environmentService.getEnvironmentByName(harnessApp.getUuid(), yaml.getEnvName());
    if (environment == null) {
      throw new WingsException("Invalid Environment name in Yaml for CVConfiguration.");
    }

    Service service = serviceResourceService.getServiceByName(harnessApp.getUuid(), yaml.getServiceName());
    if (service == null) {
      throw new WingsException("Invalid Service name in Yaml for CVConfiguration.");
    }
    bean.setAccountId(yaml.getAccountId());
    bean.setEnvId(environment.getUuid());
    bean.setEnabled24x7(yaml.isEnabled24x7());
    bean.setAnalysisTolerance(yaml.getAnalysisTolerance());
    bean.setServiceId(service.getUuid());
    bean.setAlertThreshold(yaml.getAlertThreshold());
    if (yaml.getSnoozeStartTime() != null) {
      bean.setSnoozeStartTime(yaml.getSnoozeStartTime().getTime());
    }
    if (yaml.getSnoozeEndTime() != null) {
      bean.setSnoozeEndTime(yaml.getSnoozeEndTime().getTime());
    }
    SettingAttribute connector = getConnector(yaml);
    if (connector == null) {
      throw new WingsException("Invalid connector name specified in yaml: " + yaml.getConnectorName());
    }
    bean.setConnectorId(connector.getUuid());
  }

  SettingAttribute getConnector(CVConfigurationYaml yaml) {
    return settingsService.getSettingAttributeByName(yaml.getAccountId(), yaml.getConnectorName());
  }
}
