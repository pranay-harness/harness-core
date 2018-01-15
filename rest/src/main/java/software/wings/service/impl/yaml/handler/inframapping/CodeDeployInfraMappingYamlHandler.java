package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
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
    return yaml;
  }

  @Override
  public CodeDeployInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    Yaml infraMappingYaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId);
    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + yamlFilePath, envId);
    String computeProviderId = getSettingId(accountId, appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck("Couldn't retrieve compute provider from yaml:" + yamlFilePath, computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + yamlFilePath, serviceId);

    CodeDeployInfrastructureMapping current = new CodeDeployInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    CodeDeployInfrastructureMapping previous =
        (CodeDeployInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (CodeDeployInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (CodeDeployInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private void toBean(CodeDeployInfrastructureMapping bean, ChangeContext<Yaml> context, String appId, String envId,
      String computeProviderId, String serviceId) throws HarnessException {
    Yaml infraMappingYaml = context.getYaml();
    super.toBean(context, bean, appId, envId, computeProviderId, serviceId);

    bean.setRegion(infraMappingYaml.getRegion());
    bean.setApplicationName(infraMappingYaml.getApplicationName());
    bean.setDeploymentGroup(infraMappingYaml.getDeploymentGroup());
    bean.setDeploymentConfig(infraMappingYaml.getDeploymentConfig());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getInfraMappingType())
        || isEmpty(infraMappingYaml.getServiceName()) || isEmpty(infraMappingYaml.getType())
        || isEmpty(infraMappingYaml.getRegion()) || isEmpty(infraMappingYaml.getApplicationName())
        || isEmpty(infraMappingYaml.getDeploymentGroup()) || isEmpty(infraMappingYaml.getDeploymentConfig()));
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
