package software.wings.service.impl.yaml.handler.inframapping;

import com.google.inject.Inject;

import org.mongodb.morphia.Key;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
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
public abstract class InfraMappingYamlHandler<Y extends InfrastructureMapping.Yaml, A extends InfrastructureMapping>
    extends BaseYamlHandler<Y, A> {
  @Inject SettingsService settingsService;
  @Inject EnvironmentService environmentService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject InfrastructureMappingService infraMappingService;
  @Inject ServiceTemplateService serviceTemplateService;

  protected String getSettingId(String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, settingName);
    Validator.notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute);
    return settingAttribute.getUuid();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    Validator.notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute);
    return settingAttribute.getName();
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
}
