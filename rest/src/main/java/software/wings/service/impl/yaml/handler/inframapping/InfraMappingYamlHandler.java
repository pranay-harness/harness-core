package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;

import org.mongodb.morphia.Key;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;

import java.util.List;
import java.util.Optional;

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
  @Inject InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject ServiceTemplateService serviceTemplateService;

  protected String getSettingId(String accountId, String appId, String settingName) {
    SettingAttribute settingAttribute = settingsService.getByName(accountId, appId, settingName);
    notNullCheck("Invalid SettingAttribute:" + settingName, settingAttribute, USER);
    return settingAttribute.getUuid();
  }

  protected String getEnvironmentId(String appId, String envName) {
    Environment environment = environmentService.getEnvironmentByName(appId, envName);
    notNullCheck("Invalid Environment:" + envName, environment, USER);
    return environment.getUuid();
  }

  protected String getServiceTemplateId(String appId, String serviceId, String envId) {
    List<Key<ServiceTemplate>> templateRefKeysByService =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId);
    notNullCheck("Service template can't be found for Service " + serviceId, templateRefKeysByService.get(0), USER);
    return templateRefKeysByService.get(0).getId().toString();
  }

  protected String getServiceId(String appId, String serviceName) {
    Service service = serviceResourceService.getServiceByName(appId, serviceName);
    notNullCheck("Invalid Service:" + serviceName, service, USER);
    return service.getUuid();
  }

  protected String getProvisionerId(String appId, String provisionerName) {
    if (isEmpty(provisionerName)) {
      return null;
    }

    InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerService.getByName(appId, provisionerName);
    notNullCheck("Invalid Infrastructure Provisioner:" + provisionerName, infrastructureProvisioner, USER);
    return infrastructureProvisioner.getUuid();
  }

  protected String getServiceName(String appId, String serviceId) {
    Service service = serviceResourceService.get(appId, serviceId);
    notNullCheck("Service can't be found for Id:" + serviceId, service, USER);
    return service.getName();
  }

  protected String getSettingName(String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    notNullCheck("SettingAttribute can't be found for Id:" + settingId, settingAttribute, USER);
    return settingAttribute.getName();
  }

  @Override
  public void delete(ChangeContext<Y> changeContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Optional<Environment> optionalEnvironment =
        yamlHelper.getEnvIfPresent(optionalApplication.get().getUuid(), yamlFilePath);
    if (!optionalEnvironment.isPresent()) {
      return;
    }
    InfrastructureMapping infraMapping = yamlHelper.getInfraMappingByAppIdYamlPath(
        optionalApplication.get().getUuid(), optionalEnvironment.get().getUuid(), yamlFilePath);
    if (infraMapping != null) {
      infraMappingService.deleteByYamlGit(
          optionalApplication.get().getUuid(), infraMapping.getUuid(), changeContext.getChange().isSyncFromGit());
    }
  }

  protected void toYaml(Y yaml, B infraMapping) {
    yaml.setServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()));
    yaml.setInfraMappingType(infraMapping.getInfraMappingType());
    yaml.setDeploymentType(infraMapping.getDeploymentType());
    yaml.setHarnessApiVersion(getHarnessApiVersion());
  }

  protected void toBean(ChangeContext<Y> context, B bean, String appId, String envId, String serviceId,
      String provisionerId) throws HarnessException {
    Y yaml = context.getYaml();
    bean.setAutoPopulate(false);
    bean.setInfraMappingType(yaml.getInfraMappingType());
    bean.setServiceTemplateId(getServiceTemplateId(appId, serviceId, envId));
    bean.setEnvId(envId);
    bean.setProvisionerId(provisionerId);
    bean.setServiceId(serviceId);
    bean.setDeploymentType(yaml.getDeploymentType());
    bean.setAppId(appId);
    bean.setAccountId(context.getChange().getAccountId());
    String name = yamlHelper.getNameFromYamlFilePath(context.getChange().getFilePath());
    bean.setName(name);
  }

  protected <T extends InfrastructureMapping> T upsertInfrastructureMapping(
      T current, T previous, boolean syncFromGit) {
    current.setSyncFromGit(syncFromGit);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (T) infraMappingService.update(current);
    } else {
      return (T) infraMappingService.save(current, true);
    }
  }
}
