package software.wings.service.impl.yaml.handler.InfraDefinition;

import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.Yaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Singleton
public class AwsInstanceInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<Yaml, AwsInstanceInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public Yaml toYaml(AwsInstanceInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return Yaml.builder()
        .autoScalingGroupName(bean.getAutoScalingGroupName())
        .awsInstanceFilter(bean.getAwsInstanceFilter())
        .desiredCapacity(bean.getDesiredCapacity())
        .hostConnectionAttrs(bean.getHostConnectionAttrs())
        .hostNameConvention(bean.getHostNameConvention())
        .loadBalancerName(bean.getLoadBalancerId())
        .region(bean.getRegion())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.AWS_INSTANCE)
        .build();
  }

  @Override
  public AwsInstanceInfrastructure upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsInstanceInfrastructure bean = AwsInstanceInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AwsInstanceInfrastructure bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setAutoScalingGroupName(yaml.getAutoScalingGroupName());
    bean.setAwsInstanceFilter(yaml.getAwsInstanceFilter());
    bean.setDesiredCapacity(yaml.getDesiredCapacity());
    bean.setHostConnectionAttrs(yaml.getHostConnectionAttrs());
    bean.setHostNameConvention(yaml.getHostNameConvention());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
    bean.setLoadBalancerId(yaml.getLoadBalancerName());
    bean.setRegion(yaml.getRegion());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}
