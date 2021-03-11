package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AwsLambdaInfraStructureMappingYaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
public class AwsLambdaInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<AwsLambdaInfraStructureMappingYaml,
        AwsLambdaInfraStructureMapping> {
  @Override
  public AwsLambdaInfraStructureMappingYaml toYaml(AwsLambdaInfraStructureMapping bean, String appId) {
    AwsLambdaInfraStructureMappingYaml yaml = AwsLambdaInfraStructureMappingYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.AWS_AWS_LAMBDA.name());
    yaml.setRegion(bean.getRegion());
    yaml.setVpcId(bean.getVpcId());
    yaml.setSubnetIds(bean.getSubnetIds());
    yaml.setSecurityGroupIds(bean.getSecurityGroupIds());
    yaml.setRole(bean.getRole());
    return yaml;
  }

  @Override
  public AwsLambdaInfraStructureMapping upsertFromYaml(
      ChangeContext<AwsLambdaInfraStructureMappingYaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsLambdaInfraStructureMappingYaml infraMappingYaml = changeContext.getYaml();
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

    AwsLambdaInfraStructureMapping current = new AwsLambdaInfraStructureMapping();

    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AwsLambdaInfraStructureMapping previous =
        (AwsLambdaInfraStructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(AwsLambdaInfraStructureMapping bean,
      ChangeContext<AwsLambdaInfraStructureMappingYaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) {
    AwsLambdaInfraStructureMappingYaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);

    bean.setRegion(infraMappingYaml.getRegion());
    bean.setVpcId(infraMappingYaml.getVpcId());
    bean.setSubnetIds(infraMappingYaml.getSubnetIds());
    bean.setSecurityGroupIds(infraMappingYaml.getSecurityGroupIds());
    bean.setRole(infraMappingYaml.getRole());
  }

  @Override
  public AwsLambdaInfraStructureMapping get(String accountId, String yamlFilePath) {
    return (AwsLambdaInfraStructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return AwsLambdaInfraStructureMappingYaml.class;
  }
}
