package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;

import com.google.inject.Inject;

import software.wings.beans.AmiDeploymentType;
import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsAmiInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

public class AwsAmiInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, AwsAmiInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(AwsAmiInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    String spotinstCloudProviderName = null;
    if (AmiDeploymentType.SPOTINST.equals(bean.getAmiDeploymentType())) {
      SettingAttribute spotinstCloudProvider = settingsService.get(bean.getSpotinstCloudProvider());
      notNullCheck(
          "SettingAttribute can't be found for Id:" + bean.getSpotinstCloudProvider(), spotinstCloudProvider, USER);
      spotinstCloudProviderName = spotinstCloudProvider.getName();
    }

    return Yaml.builder()
        .autoScalingGroupName(bean.getAutoScalingGroupName())
        .classicLoadBalancers(bean.getClassicLoadBalancers())
        .hostNameConvention(bean.getHostNameConvention())
        .region(bean.getRegion())
        .stageClassicLoadBalancers(bean.getStageClassicLoadBalancers())
        .stageTargetGroupArns(bean.getStageTargetGroupArns())
        .targetGroupArns(bean.getTargetGroupArns())
        .cloudProviderName(cloudProvider.getName())
        .amiDeploymentType(bean.getAmiDeploymentType())
        .spotinstCloudProviderName(spotinstCloudProviderName)
        .spotinstElastiGroupJson(bean.getSpotinstElastiGroupJson())
        .asgIdentifiesWorkload(bean.isAsgIdentifiesWorkload())
        .type(InfrastructureType.AWS_AMI)
        .expressions(bean.getExpressions())
        .build();
  }

  @Override
  public AwsAmiInfrastructure upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsAmiInfrastructure bean = AwsAmiInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AwsAmiInfrastructure bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    String spotinstCloudProviderId = null;
    if (AmiDeploymentType.SPOTINST.equals(yaml.getAmiDeploymentType())) {
      SettingAttribute spotinstCloudProvider =
          settingsService.getSettingAttributeByName(accountId, yaml.getSpotinstCloudProviderName());
      notNullCheck("SettingAttribute can't be found for Name:" + yaml.getSpotinstCloudProviderName(),
          spotinstCloudProvider, USER);
      spotinstCloudProviderId = spotinstCloudProvider.getUuid();
    }
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setRegion(yaml.getRegion());
    bean.setAutoScalingGroupName(yaml.getAutoScalingGroupName());
    bean.setClassicLoadBalancers(yaml.getClassicLoadBalancers());
    bean.setHostNameConvention(yaml.getHostNameConvention());
    bean.setAmiDeploymentType(yaml.getAmiDeploymentType());
    bean.setSpotinstCloudProvider(spotinstCloudProviderId);
    bean.setSpotinstElastiGroupJson(yaml.getSpotinstElastiGroupJson());
    bean.setAsgIdentifiesWorkload(yaml.isAsgIdentifiesWorkload());
    bean.setExpressions(yaml.getExpressions());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
