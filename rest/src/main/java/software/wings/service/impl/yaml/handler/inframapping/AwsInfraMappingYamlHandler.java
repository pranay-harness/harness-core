package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.collect.Lists;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.Yaml;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/15/17
 */
public class AwsInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, AwsInfrastructureMapping> {
  @Override
  public Yaml toYaml(AwsInfrastructureMapping bean, String appId) {
    AwsInstanceFilter awsInstanceFilter = bean.getAwsInstanceFilter();
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

    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);

    String hostConnectionAttrsSettingId = bean.getHostConnectionAttrs();

    SettingAttribute settingAttribute = settingsService.get(hostConnectionAttrsSettingId);
    Validator.notNullCheck(
        "Host connection attributes null for the given id: " + hostConnectionAttrsSettingId, settingAttribute);
    yaml.setConnectionType(settingAttribute.getName());

    yaml.setType(InfrastructureMappingType.AWS_SSH.name());
    yaml.setRestrictions(bean.getRestrictionType());
    yaml.setExpression(bean.getRestrictionExpression());
    yaml.setRegion(bean.getRegion());
    yaml.setLoadBalancer(bean.getLoadBalancerName());
    yaml.setUsePublicDns(bean.isUsePublicDns());
    yaml.setProvisionInstances(bean.isProvisionInstances());
    yaml.setAutoScalingGroup(bean.getAutoScalingGroupName());
    yaml.setDesiredCapacity(bean.getDesiredCapacity());
    yaml.setVpcs(vpcIds);
    yaml.setSubnetIds(subnetIds);
    yaml.setSecurityGroupIds(securityGroupIds);
    yaml.setTags(getTagsYaml(tagList));
    return yaml;
  }

  @Override
  public AwsInfrastructureMapping upsertFromYaml(
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

    AwsInfrastructureMapping current = new AwsInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AwsInfrastructureMapping previous =
        (AwsInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return (AwsInfrastructureMapping) infraMappingService.update(current);
    } else {
      return (AwsInfrastructureMapping) infraMappingService.save(current);
    }
  }

  private List<NameValuePair.Yaml> getTagsYaml(List<Tag> tagList) {
    return tagList.stream()
        .map(tag -> NameValuePair.Yaml.builder().name(tag.getKey()).value(tag.getValue()).build())
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

  private void toBean(AwsInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    AwsInstanceFilter awsInstanceFilter = new AwsInstanceFilter();
    awsInstanceFilter.setSecurityGroupIds(yaml.getSecurityGroupIds());
    awsInstanceFilter.setSubnetIds(yaml.getSubnetIds());
    awsInstanceFilter.setVpcIds(yaml.getVpcs());
    if (yaml.getTags() != null) {
      awsInstanceFilter.setTags(getTags(yaml.getTags()));
    }

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId);

    String hostConnAttrsName = yaml.getConnectionType();
    SettingAttribute hostConnAttributes =
        settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), hostConnAttrsName);
    Validator.notNullCheck("HostConnectionAttrs is null for name:" + hostConnAttrsName, hostConnAttributes);
    bean.setHostConnectionAttrs(hostConnAttributes.getUuid());

    bean.setRestrictionType(yaml.getRestrictions());
    bean.setRestrictionExpression(yaml.getExpression());
    bean.setRegion(yaml.getRegion());
    bean.setLoadBalancerId(yaml.getLoadBalancer());
    bean.setLoadBalancerName(yaml.getLoadBalancer());
    bean.setUsePublicDns(yaml.isUsePublicDns());
    bean.setProvisionInstances(yaml.isProvisionInstances());
    bean.setAutoScalingGroupName(yaml.getAutoScalingGroup());
    bean.setDesiredCapacity(yaml.getDesiredCapacity());
    bean.setAwsInstanceFilter(awsInstanceFilter);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml infraMappingYaml = changeContext.getYaml();
    return !(isEmpty(infraMappingYaml.getComputeProviderName()) || isEmpty(infraMappingYaml.getComputeProviderType())
        || isEmpty(infraMappingYaml.getDeploymentType()) || isEmpty(infraMappingYaml.getInfraMappingType())
        || isEmpty(infraMappingYaml.getServiceName()) || isEmpty(infraMappingYaml.getType())
        || isEmpty(infraMappingYaml.getRegion()) || isEmpty(infraMappingYaml.getConnectionType()));
  }

  @Override
  public AwsInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (AwsInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
