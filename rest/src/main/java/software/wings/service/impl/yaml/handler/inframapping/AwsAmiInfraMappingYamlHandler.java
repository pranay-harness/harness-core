package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
public class AwsAmiInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, AwsAmiInfrastructureMapping> {
  @Override
  public Yaml toYaml(AwsAmiInfrastructureMapping bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureMappingType.AWS_AMI.name());
    yaml.setRegion(bean.getRegion());
    yaml.setAutoScalingGroupName(bean.getAutoScalingGroupName());
    yaml.setClassicLoadBalancers(bean.getClassicLoadBalancers());
    yaml.setTargetGroupArns(bean.getTargetGroupArns());
    return yaml;
  }

  @Override
  public AwsAmiInfrastructureMapping upsertFromYaml(
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

    AwsAmiInfrastructureMapping current = new AwsAmiInfrastructureMapping();

    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AwsAmiInfrastructureMapping previous =
        (AwsAmiInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (AwsAmiInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (AwsAmiInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private void toBean(AwsAmiInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);

    bean.setRegion(infraMappingYaml.getRegion());
    bean.setAutoScalingGroupName(infraMappingYaml.getAutoScalingGroupName());
    bean.setClassicLoadBalancers(infraMappingYaml.getClassicLoadBalancers());
    bean.setTargetGroupArns(infraMappingYaml.getTargetGroupArns());
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getInfraMappingType())
        || isEmpty(infraMappingYaml.getServiceName()) || isEmpty(infraMappingYaml.getType())
        || isEmpty(infraMappingYaml.getRegion()) || isEmpty(infraMappingYaml.getAutoScalingGroupName()));
  }

  @Override
  public AwsAmiInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (AwsAmiInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
