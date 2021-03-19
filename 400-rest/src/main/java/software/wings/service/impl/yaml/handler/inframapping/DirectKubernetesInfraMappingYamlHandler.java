package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMappingYaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class DirectKubernetesInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<DirectKubernetesInfrastructureMappingYaml,
        DirectKubernetesInfrastructureMapping> {
  @Override
  public DirectKubernetesInfrastructureMappingYaml toYaml(DirectKubernetesInfrastructureMapping bean, String appId) {
    DirectKubernetesInfrastructureMappingYaml yaml = DirectKubernetesInfrastructureMappingYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.DIRECT_KUBERNETES.name());
    yaml.setNamespace(bean.getNamespace());
    yaml.setReleaseName(bean.getReleaseName());
    return yaml;
  }

  @Override
  public DirectKubernetesInfrastructureMapping upsertFromYaml(
      ChangeContext<DirectKubernetesInfrastructureMappingYaml> changeContext, List<ChangeContext> changeSetContext) {
    DirectKubernetesInfrastructureMappingYaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    DirectKubernetesInfrastructureMapping current = new DirectKubernetesInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    DirectKubernetesInfrastructureMapping previous =
        (DirectKubernetesInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(DirectKubernetesInfrastructureMapping bean,
      ChangeContext<DirectKubernetesInfrastructureMappingYaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) {
    DirectKubernetesInfrastructureMappingYaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, serviceId, null);
    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);
    bean.setNamespace(infraMappingYaml.getNamespace());
    bean.setReleaseName(infraMappingYaml.getReleaseName());

    // Hardcoding it to some value since its a not null field in db. This field was used in name generation logic, but
    // no more.
    bean.setClusterName("clusterName");
  }

  @Override
  public DirectKubernetesInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (DirectKubernetesInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return DirectKubernetesInfrastructureMappingYaml.class;
  }
}
