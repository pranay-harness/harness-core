/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.inframapping;

import static io.harness.annotations.dev.HarnessModule._955_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfraMappingYaml;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

/**
 * @author rktummala on 10/22/17
 */
@Singleton
@OwnedBy(CDP)
@TargetModule(_955_CG_YAML)
public class AwsAmiInfraMappingYamlHandler
    extends InfraMappingYamlWithComputeProviderHandler<AwsAmiInfraMappingYaml, AwsAmiInfrastructureMapping> {
  @Override
  public AwsAmiInfraMappingYaml toYaml(AwsAmiInfrastructureMapping bean, String appId) {
    AwsAmiInfraMappingYaml yaml = AwsAmiInfraMappingYaml.builder().build();
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
      ChangeContext<AwsAmiInfraMappingYaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsAmiInfraMappingYaml infraMappingYaml = changeContext.getYaml();
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

  private void toBean(AwsAmiInfrastructureMapping bean, ChangeContext<AwsAmiInfraMappingYaml> changeContext,
      String appId, String envId, String computeProviderId, String serviceId, String spotinstCloudProviderId) {
    AwsAmiInfraMappingYaml infraMappingYaml = changeContext.getYaml();

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
    return AwsAmiInfraMappingYaml.class;
  }
}
