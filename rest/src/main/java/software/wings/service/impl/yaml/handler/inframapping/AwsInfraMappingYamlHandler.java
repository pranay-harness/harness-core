package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping.Yaml;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterBuilder;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;

import java.util.List;

/**
 * @author rktummala on 10/15/17
 */
@Singleton
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
    // For dynamic provisioning, hostConnectionAttrs can be null.
    // There you just need hostNames as provisioning step will give asg
    if (!avoidSettingHostConnAttributesToYaml(bean)) {
      SettingAttribute settingAttribute = settingsService.get(hostConnectionAttrsSettingId);

      notNullCheck(
          "Host connection attributes null for the given id: " + hostConnectionAttrsSettingId, settingAttribute, USER);
      yaml.setConnectionType(settingAttribute.getName());
    }
    yaml.setType(InfrastructureMappingType.AWS_SSH.name());
    yaml.setRestrictions(bean.getRestrictionType());
    yaml.setExpression(bean.getRestrictionExpression());
    yaml.setLoadBalancer(bean.getLoadBalancerName());
    yaml.setUsePublicDns(bean.isUsePublicDns());
    yaml.setProvisionInstances(bean.isProvisionInstances());
    yaml.setAutoScalingGroup(bean.getAutoScalingGroupName());
    yaml.setDesiredCapacity(bean.getDesiredCapacity());

    if (bean.getProvisionerId() == null) {
      yaml.setRegion(bean.getRegion());
      yaml.setVpcs(vpcIds);
      yaml.setSubnetIds(subnetIds);
      yaml.setSecurityGroupIds(securityGroupIds);
      yaml.setTags(getTagsYaml(tagList));
    } else {
      final InfrastructureProvisioner infrastructureProvisioner =
          infrastructureProvisionerService.get(appId, bean.getProvisionerId());
      notNullCheck("Missing provisioner", infrastructureProvisioner);
      yaml.setProvisionerName(infrastructureProvisioner.getName());
    }

    yaml.setHostNameConvention(bean.getHostNameConvention());
    return yaml;
  }

  @Override
  public AwsInfrastructureMapping upsertFromYaml(
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
    String provisionerId = getProvisionerId(appId, infraMappingYaml.getProvisionerName());

    AwsInfrastructureMapping current = new AwsInfrastructureMapping();
    toBean(current, changeContext, appId, envId, computeProviderId, serviceId, provisionerId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    AwsInfrastructureMapping previous =
        (AwsInfrastructureMapping) infraMappingService.getInfraMappingByName(appId, envId, name);

    return upsertInfrastructureMapping(current, previous, changeContext.getChange().isSyncFromGit());
  }

  private List<NameValuePair.Yaml> getTagsYaml(List<Tag> tagList) {
    if (tagList == null) {
      return null;
    }

    return tagList.stream()
        .map(tag -> NameValuePair.Yaml.builder().name(tag.getKey()).value(tag.getValue()).build())
        .collect(toList());
  }

  private List<Tag> getTags(List<NameValuePair.Yaml> tagYamlList) {
    return tagYamlList.stream()
        .map(tagYaml -> Tag.builder().key(tagYaml.getName()).value(tagYaml.getValue()).build())
        .collect(toList());
  }

  private void toBean(AwsInfrastructureMapping bean, ChangeContext<Yaml> changeContext, String appId, String envId,
      String computeProviderId, String serviceId, String provisionerId) throws HarnessException {
    Yaml yaml = changeContext.getYaml();

    AwsInstanceFilterBuilder builder = AwsInstanceFilter.builder()
                                           .securityGroupIds(yaml.getSecurityGroupIds())
                                           .subnetIds(yaml.getSubnetIds())
                                           .vpcIds(yaml.getVpcs());
    if (yaml.getTags() != null) {
      builder.tags(getTags(yaml.getTags()));
    }

    super.toBean(changeContext, bean, appId, envId, computeProviderId, serviceId, provisionerId);

    if (!avoidSettingHostConnAttributesFromYaml(yaml)) {
      String hostConnAttrsName = yaml.getConnectionType();
      SettingAttribute hostConnAttributes =
          settingsService.getSettingAttributeByName(changeContext.getChange().getAccountId(), hostConnAttrsName);
      notNullCheck("HostConnectionAttrs is null for name:" + hostConnAttrsName, hostConnAttributes, USER);
      bean.setHostConnectionAttrs(hostConnAttributes.getUuid());
    }

    bean.setRestrictionType(yaml.getRestrictions());
    bean.setRestrictionExpression(yaml.getExpression());
    bean.setRegion(yaml.getRegion());
    bean.setLoadBalancerId(yaml.getLoadBalancer());
    bean.setLoadBalancerName(yaml.getLoadBalancer());
    bean.setUsePublicDns(yaml.isUsePublicDns());
    bean.setProvisionInstances(yaml.isProvisionInstances());
    bean.setAutoScalingGroupName(yaml.getAutoScalingGroup());
    bean.setDesiredCapacity(yaml.getDesiredCapacity());
    bean.setAwsInstanceFilter(builder.build());
    bean.setHostNameConvention(yaml.getHostNameConvention());
  }

  @Override
  public AwsInfrastructureMapping get(String accountId, String yamlFilePath) {
    return (AwsInfrastructureMapping) yamlHelper.getInfraMapping(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  private boolean avoidSettingHostConnAttributesToYaml(AwsInfrastructureMapping bean) {
    if (isNotEmpty(bean.getProvisionerId()) && isEmpty(bean.getHostConnectionAttrs())) {
      return true;
    } else {
      return false;
    }
  }

  private boolean avoidSettingHostConnAttributesFromYaml(Yaml yaml) {
    if (isEmpty(yaml.getConnectionType()) && isNotEmpty(yaml.getProvisionerName())) {
      return true;
    }
    return false;
  }
}
