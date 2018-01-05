package software.wings.service.impl.yaml.handler.inframapping;

import com.google.inject.Inject;

import org.mongodb.morphia.Key;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/15/17
 */
public abstract class InfraMappingYamlHandler<Y extends InfrastructureMapping.Yaml, B extends InfrastructureMapping>
    extends BaseYamlHandler<Y, B> {
  @Inject SettingsService settingsService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject YamlHelper yamlHelper;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ServiceTemplateService serviceTemplateService;

  protected String getSettingId(String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, settingName);
    Validator.notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute);
    return settingAttribute.getUuid();
  }

  protected String getEnvironmentId(String appId, String envName) {
    Environment environment = environmentService.getEnvironmentByName(appId, envName);
    Validator.notNullCheck("Invalid Environment:" + envName, environment);
    return environment.getUuid();
  }

  protected String getServiceTemplateId(String appId, String serviceId) {
    List<Key<ServiceTemplate>> templateRefKeysByService =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, null);
    Validator.notNullCheck("Service template can't be found for Service " + serviceId, templateRefKeysByService.get(0));
    return templateRefKeysByService.get(0).getId().toString();
  }

  protected String getServiceId(String appId, String serviceName) {
    Service service = serviceResourceService.getServiceByName(appId, serviceName);
    Validator.notNullCheck("Invalid Service:" + serviceName, service);
    return service.getUuid();
  }

  protected String getServiceName(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    Validator.notNullCheck("Service can't be found for Id:" + serviceId, service);
    return service.getName();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    Validator.notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute);
    return settingAttribute.getName();
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Application can't be found for yaml file:" + yamlFilePath, appId);
    InfrastructureMapping infraMapping = yamlHelper.getInfraMapping(accountId, yamlFilePath);
    if (infraMapping != null) {
      infraMappingService.delete(appId, infraMapping.getUuid());
    }
  }

  protected void toYaml(Y yaml, B infraMapping) {
    yaml.setServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()));
    yaml.setInfraMappingType(infraMapping.getInfraMappingType());
    yaml.setDeploymentType(infraMapping.getDeploymentType());
    yaml.setHarnessApiVersion("1.0");
  }

  protected void toBean(ChangeContext<Y> context, B bean, String appId, String envId, String serviceId)
      throws HarnessException {
    Y yaml = context.getYaml();
    bean.setAutoPopulate(false);
    bean.setInfraMappingType(yaml.getInfraMappingType());
    bean.setServiceTemplateId(getServiceTemplateId(appId, serviceId));
    bean.setEnvId(envId);
    bean.setServiceId(serviceId);
    bean.setDeploymentType(yaml.getDeploymentType());
    bean.setAppId(appId);
    bean.setAccountId(context.getChange().getAccountId());
    String name = yamlHelper.getNameFromYamlFilePath(context.getChange().getFilePath());
    bean.setName(name);
  }
}
