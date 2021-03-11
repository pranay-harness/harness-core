package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AwsInstanceInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<AwsInstanceInfrastructureYaml, AwsInstanceInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public AwsInstanceInfrastructureYaml toYaml(AwsInstanceInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    SettingAttribute hostNameConnectionAttr = settingsService.get(bean.getHostConnectionAttrs());
    return AwsInstanceInfrastructureYaml.builder()
        .autoScalingGroupName(bean.getAutoScalingGroupName())
        .awsInstanceFilter(bean.getAwsInstanceFilter())
        .desiredCapacity(bean.getDesiredCapacity())
        .hostNameConvention(bean.getHostNameConvention())
        .loadBalancerName(bean.getLoadBalancerId())
        .region(bean.getRegion())
        .cloudProviderName(cloudProvider.getName())
        .hostConnectionAttrsName(hostNameConnectionAttr.getName())
        .type(InfrastructureType.AWS_INSTANCE)
        .hostConnectionType(bean.getHostConnectionType())
        .expressions(bean.getExpressions())
        .useAutoScalingGroup(bean.isProvisionInstances())
        .build();
  }

  @Override
  public AwsInstanceInfrastructure upsertFromYaml(
      ChangeContext<AwsInstanceInfrastructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsInstanceInfrastructure bean = AwsInstanceInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AwsInstanceInfrastructure bean, ChangeContext<AwsInstanceInfrastructureYaml> changeContext) {
    AwsInstanceInfrastructureYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    SettingAttribute hostConnectionAttr =
        settingsService.getSettingAttributeByName(accountId, yaml.getHostConnectionAttrsName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    notNullCheck(format("Connection Attribute with name %s does not exist", yaml.getHostConnectionAttrsName()),
        hostConnectionAttr);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setAutoScalingGroupName(yaml.getAutoScalingGroupName());
    bean.setAwsInstanceFilter(yaml.getAwsInstanceFilter());
    bean.setDesiredCapacity(yaml.getDesiredCapacity());
    bean.setHostConnectionAttrs(hostConnectionAttr.getUuid());
    bean.setHostNameConvention(yaml.getHostNameConvention());
    bean.setLoadBalancerName(yaml.getLoadBalancerName());
    bean.setLoadBalancerId(yaml.getLoadBalancerName());
    bean.setRegion(yaml.getRegion());
    bean.setExpressions(yaml.getExpressions());
    bean.setProvisionInstances(yaml.isUseAutoScalingGroup());
    bean.setHostConnectionType(yaml.getHostConnectionType());
  }

  @Override
  public Class getYamlClass() {
    return AwsInstanceInfrastructureYaml.class;
  }
}
