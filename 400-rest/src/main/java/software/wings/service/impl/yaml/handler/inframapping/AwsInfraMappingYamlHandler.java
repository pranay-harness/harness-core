/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

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

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/15/17
 */
@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class AwsInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<Yaml, AwsInfrastructureMapping> {
  @Override
  public Yaml toYaml(AwsInfrastructureMapping bean, String appId) {
    AwsInstanceFilter awsInstanceFilter = bean.getAwsInstanceFilter();
    List<String> vpcIds = Lists.newArrayList();
    List<Tag> tagList = Lists.newArrayList();
    if (awsInstanceFilter != null) {
      vpcIds = awsInstanceFilter.getVpcIds();
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
    yaml.setHostConnectionType(bean.getHostConnectionType());
    yaml.setProvisionInstances(bean.isProvisionInstances());
    yaml.setAutoScalingGroup(bean.getAutoScalingGroupName());
    yaml.setDesiredCapacity(bean.getDesiredCapacity());

    if (bean.getProvisionerId() == null) {
      yaml.setRegion(bean.getRegion());
      yaml.setVpcs(vpcIds);
      yaml.setAwsTags(getTagsYaml(tagList));
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
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
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
      String computeProviderId, String serviceId, String provisionerId) {
    Yaml yaml = changeContext.getYaml();

    AwsInstanceFilterBuilder builder = AwsInstanceFilter.builder().vpcIds(yaml.getVpcs());
    if (yaml.getAwsTags() != null) {
      builder.tags(getTags(yaml.getAwsTags()));
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
    bean.setHostConnectionType(yaml.getHostConnectionType());
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
    return isNotEmpty(bean.getProvisionerId()) && isEmpty(bean.getHostConnectionAttrs());
  }

  private boolean avoidSettingHostConnAttributesFromYaml(Yaml yaml) {
    return isEmpty(yaml.getConnectionType()) && isNotEmpty(yaml.getProvisionerName());
  }
}
