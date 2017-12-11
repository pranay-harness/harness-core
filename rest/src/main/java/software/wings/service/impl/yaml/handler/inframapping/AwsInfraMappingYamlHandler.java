package software.wings.service.impl.yaml.handler.inframapping;

import static software.wings.beans.AwsInfrastructureMapping.Yaml.Builder.aYaml;
import static software.wings.utils.Util.isEmpty;

import com.google.common.collect.Lists;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.Yaml;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.NameValuePair;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/15/17
 */
public class AwsInfraMappingYamlHandler
    extends InfraMappingYamlHandler<AwsInfrastructureMapping.Yaml, AwsInfrastructureMapping> {
  @Override
  public AwsInfrastructureMapping.Yaml toYaml(AwsInfrastructureMapping infraMapping, String appId) {
    AwsInstanceFilter awsInstanceFilter = infraMapping.getAwsInstanceFilter();
    List<String> vpcIds = Lists.newArrayList();
    List<String> subnetIds = Lists.newArrayList();
    List<String> securityGroupIds = Lists.newArrayList();
    List<Tag> tagList = Lists.newArrayList();
    if (awsInstanceFilter != null) {
      vpcIds = awsInstanceFilter.getVpcIds();
      subnetIds = awsInstanceFilter.getSubnetIds();
      securityGroupIds = awsInstanceFilter.getSecurityGroupIds();
      tagList = awsInstanceFilter.getTags();
    }

    return aYaml()
        .withType(InfrastructureMappingType.AWS_SSH.name())
        .withRestrictions(infraMapping.getRestrictionType())
        .withComputeProviderType(infraMapping.getComputeProviderType())
        .withExpression(infraMapping.getRestrictionExpression())
        .withRegion(infraMapping.getRegion())
        .withServiceName(getServiceName(infraMapping.getAppId(), infraMapping.getServiceId()))
        .withInfraMappingType(infraMapping.getInfraMappingType())
        .withConnectionType(infraMapping.getHostConnectionAttrs())
        .withDeploymentType(infraMapping.getDeploymentType())
        .withLoadBalancer(infraMapping.getLoadBalancerName())
        .withComputeProviderName(infraMapping.getComputeProviderName())
        .withName(infraMapping.getName())
        .withUsePublicDns(infraMapping.isUsePublicDns())
        .withProvisionInstances(infraMapping.isProvisionInstances())
        .withAutoScalingGroup(infraMapping.getAutoScalingGroupName())
        .withDesiredCapacity(infraMapping.getDesiredCapacity())
        .withVpcs(vpcIds)
        .withSubnetIds(subnetIds)
        .withSecurityGroupIds(securityGroupIds)
        .withTags(getTagsYaml(tagList))
        .build();
  }

  @Override
  public AwsInfrastructureMapping upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    ensureValidChange(changeContext, changeSetContext);

    AwsInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();

    String appId =
        yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + changeContext.getChange().getFilePath(), appId);
    String envId = yamlSyncHelper.getEnvironmentId(appId, changeContext.getChange().getFilePath());
    Validator.notNullCheck("Couldn't retrieve environment from yaml:" + changeContext.getChange().getFilePath(), envId);
    String computeProviderId = getSettingId(appId, infraMappingYaml.getComputeProviderName());
    Validator.notNullCheck(
        "Couldn't retrieve compute provider from yaml:" + changeContext.getChange().getFilePath(), computeProviderId);
    String serviceId = getServiceId(appId, infraMappingYaml.getServiceName());
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + changeContext.getChange().getFilePath(), serviceId);

    AwsInfrastructureMapping.Builder builder =
        AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().withAccountId(
            changeContext.getChange().getAccountId());
    setWithYamlValues(builder, infraMappingYaml, appId, envId, computeProviderId, serviceId);
    AwsInfrastructureMapping current = builder.build();

    AwsInfrastructureMapping previous =
        (AwsInfrastructureMapping) infraMappingService.getInfraMappingByComputeProviderAndServiceId(
            appId, envId, serviceId, computeProviderId);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (AwsInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (AwsInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private List<NameValuePair.Yaml> getTagsYaml(List<Tag> tagList) {
    return tagList.stream()
        .map(tag -> NameValuePair.Yaml.Builder.aYaml().withName(tag.getKey()).withValue(tag.getValue()).build())
        .collect(Collectors.toList());
  }

  private List<Tag> getTags(List<NameValuePair.Yaml> tagYamlList) {
    return tagYamlList.stream()
        .map(tagYaml -> {
          Tag tag = new Tag();
          tag.setKey(tagYaml.getName());
          tag.setValue(tagYaml.getValue());
          return tag;
        })
        .collect(Collectors.toList());
  }

  @Override
  public AwsInfrastructureMapping updateFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  private void setWithYamlValues(AwsInfrastructureMapping.Builder builder,
      AwsInfrastructureMapping.Yaml infraMappingYaml, String appId, String envId, String computeProviderId,
      String serviceId) {
    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.setSecurityGroupIds(infraMappingYaml.getSecurityGroupIds());
    awsInstanceFilter.setSubnetIds(infraMappingYaml.getSubnetIds());
    awsInstanceFilter.setVpcIds(infraMappingYaml.getVpcs());
    if (infraMappingYaml.getTags() != null) {
      awsInstanceFilter.setTags(getTags(infraMappingYaml.getTags()));
    }

    builder.withAutoPopulate(false)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withServiceTemplateId(getServiceTemplateId(appId, serviceId))
        .withComputeProviderSettingId(computeProviderId)
        .withComputeProviderName(infraMappingYaml.getComputeProviderName())
        .withComputeProviderType(infraMappingYaml.getComputeProviderType())
        .withEnvId(envId)
        .withServiceId(serviceId)
        .withInfraMappingType(infraMappingYaml.getInfraMappingType())
        .withDeploymentType(infraMappingYaml.getDeploymentType())
        .withName(infraMappingYaml.getName())
        .withAppId(appId);

    builder.withRestrictionType(infraMappingYaml.getRestrictions())
        .withRestrictionExpression(infraMappingYaml.getExpression())
        .withRegion(infraMappingYaml.getRegion())
        .withHostConnectionAttrs(infraMappingYaml.getConnectionType())
        .withLoadBalancerId(infraMappingYaml.getLoadBalancer())
        .withLoadBalancerName(infraMappingYaml.getLoadBalancer())
        .withName(infraMappingYaml.getName())
        .withUsePublicDns(infraMappingYaml.isUsePublicDns())
        .withProvisionInstances(infraMappingYaml.isProvisionInstances())
        .withAutoScalingGroupName(infraMappingYaml.getAutoScalingGroup())
        .withDesiredCapacity(infraMappingYaml.getDesiredCapacity())
        .withAwsInstanceFilter(awsInstanceFilter);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsInfrastructureMapping.Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getName())
        || isEmpty(infraMappingYaml.getInfraMappingType()) || isEmpty(infraMappingYaml.getServiceName())
        || isEmpty(infraMappingYaml.getType()) || isEmpty(infraMappingYaml.getRegion())
        || isEmpty(infraMappingYaml.getConnectionType()));
  }

  @Override
  public AwsInfrastructureMapping createFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) throws HarnessException {
    return upsertFromYaml(changeContext, changeSetContext);
  }

  @Override
  public AwsInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (AwsInfrastructureMapping) yamlSyncHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return AwsInfrastructureMapping.Yaml.class;
  }
}
