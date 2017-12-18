package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.utils.Util.isEmpty;

import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class DirectKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlHandler<Yaml, DirectKubernetesInfrastructureMapping> {
  @Override
  public Yaml toYaml(DirectKubernetesInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.DIRECT_KUBERNETES.name());
    yaml.setMasterUrl(bean.getMasterUrl());
    yaml.setUsername(bean.getUsername());
    yaml.setPassword(new String(bean.getPassword()));
    yaml.setNamespace(bean.getNamespace());
    return yaml;
  }

  @Override
  public DirectKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId);
    String computeProviderId = getSettingId(appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId);

    DirectKubernetesInfrastructureMapping current = new DirectKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);
    DirectKubernetesInfrastructureMapping previous =
        (DirectKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (DirectKubernetesInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (DirectKubernetesInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private void toBean(DirectKubernetesInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId,
      String envId, String computeProviderId, String serviceId) {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);
    bean.setMasterUrl(infraMappingYaml.getMasterUrl());
    bean.setUsername(infraMappingYaml.getUsername());
    bean.setPassword(infraMappingYaml.getPassword().toCharArray());
    bean.setEncryptedPassword(infraMappingYaml.getPassword());
    bean.setNamespace(infraMappingYaml.getNamespace());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getInfraMappingType())
        || isEmpty(infraMappingYaml.getServiceName()) || isEmpty(infraMappingYaml.getType())
        || isEmpty(infraMappingYaml.getMasterUrl()) || isEmpty(infraMappingYaml.getUsername())
        || isEmpty(infraMappingYaml.getPassword().toString()) || isEmpty(infraMappingYaml.getNamespace()));
  }

  @Override
  public DirectKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (DirectKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
