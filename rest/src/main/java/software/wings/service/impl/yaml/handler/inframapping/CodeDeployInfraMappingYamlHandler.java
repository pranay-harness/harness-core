package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Singleton;

import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class CodeDeployInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, CodeDeployInfrastructureMapping> {
  @Override
  public Yaml toYaml(CodeDeployInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name());
    yaml.setRegion(bean.getRegion());
    yaml.setApplicationName(bean.getApplicationName());
    yaml.setDeploymentGroup(bean.getDeploymentGroup());
    yaml.setDeploymentConfig(bean.getDeploymentConfig());
    yaml.setHostNameConvention(bean.getHostNameConvention());
    return yaml;
  }

  @Override
  public CodeDeployInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId, USER);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId, USER);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId, USER);

    CodeDeployInfrastructureMapping current = new CodeDeployInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    CodeDeployInfrastructureMapping previous =
        (CodeDeployInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous);
  }

  private void toBean(CodeDeployInfrastructureMapping bean, ChangeContext<Yaml> context, String appId, String envId,
      String computeProviderId, String serviceId) throws HarnessException {
    Yaml infraMappingYaml = context.getYaml();
    super.toBean(context, bean, appId, envId, computeProviderId, serviceId, null);

    bean.setRegion(infraMappingYaml.getRegion());
    bean.setApplicationName(infraMappingYaml.getApplicationName());
    bean.setDeploymentGroup(infraMappingYaml.getDeploymentGroup());
    bean.setDeploymentConfig(infraMappingYaml.getDeploymentConfig());
    bean.setHostNameConvention(infraMappingYaml.getHostNameConvention());
  }

  @Override
  public CodeDeployInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (CodeDeployInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
