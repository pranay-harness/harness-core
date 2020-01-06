package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsAmiInfrastructureMapping.Yaml;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
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
    yaml.setHostNameConvention(bean.getHostNameConvention());
    yaml.setStageClassicLoadBalancers(bean.getStageClassicLoadBalancers());
    yaml.setStageTargetGroupArns(bean.getStageTargetGroupArns());
    yaml.setAmiDeploymentType(bean.getAmiDeploymentType());
    yaml.setSpotinstElastiGroupJson(bean.getSpotinstElastiGroupJson());
    if (AmiDeploymentType.SPOTINST == bean.getAmiDeploymentType()) {
      yaml.setSpotinstCloudProviderName(getSettingName(bean.getSpotinstCloudProvider()));
    }
    return yaml;
  }

  @Override
  public AwsAmiInfrastructureMapping upsertFromYaml(
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
    String spotinstCloudProviderId = null;
    if (AmiDeploymentType.SPOTINST == infraMappingYaml.getAmiDeploymentType()) {
      spotinstCloudProviderId = getSettingId(accountId, appId, infraMappingYaml.getSpotinstCloudProviderName());
    }

    AwsAmiInfrastructureMapping current = new AwsAmiInfrastructureMapping();

    toBean(current, changeContext, appId, envId, computeProviderId, serviceId, spotinstCloudProviderId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AwsAmiInfrastructureMapping previous =
        (AwsAmiInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private void toBean(AwsAmiInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId, String spotinstCloudProviderId) throws HarnessException {
    Yaml infraMappingYaml = changeContext.getYaml();

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, null);

    bean.setRegion(infraMappingYaml.getRegion());
    bean.setAutoScalingGroupName(infraMappingYaml.getAutoScalingGroupName());
    bean.setClassicLoadBalancers(infraMappingYaml.getClassicLoadBalancers());
    bean.setTargetGroupArns(infraMappingYaml.getTargetGroupArns());
    bean.setHostNameConvention(infraMappingYaml.getHostNameConvention());
    bean.setStageClassicLoadBalancers(infraMappingYaml.getStageClassicLoadBalancers());
    bean.setStageTargetGroupArns(infraMappingYaml.getStageTargetGroupArns());
    bean.setAmiDeploymentType(infraMappingYaml.getAmiDeploymentType());
    bean.setSpotinstElastiGroupJson(infraMappingYaml.getSpotinstElastiGroupJson());
    bean.setSpotinstCloudProvider(spotinstCloudProviderId);
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
