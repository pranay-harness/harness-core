package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AwsLambdaInfrastructureYaml;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
public class AwsLambdaInfrastructureYamlHandler
    extends CloudProviderInfrastructureYamlHandler<AwsLambdaInfrastructureYaml, AwsLambdaInfrastructure> {
  @Inject private SettingsService settingsService;
  @Override
  public AwsLambdaInfrastructureYaml toYaml(AwsLambdaInfrastructure bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    return AwsLambdaInfrastructureYaml.builder()
        .iamRole(bean.getRole())
        .region(bean.getRegion())
        .securityGroupIds(bean.getSecurityGroupIds())
        .subnetIds(bean.getSubnetIds())
        .vpcId(bean.getVpcId())
        .cloudProviderName(cloudProvider.getName())
        .type(InfrastructureType.AWS_LAMBDA)
        .expressions(bean.getExpressions())
        .build();
  }

  @Override
  public AwsLambdaInfrastructure upsertFromYaml(
      ChangeContext<AwsLambdaInfrastructureYaml> changeContext, List<ChangeContext> changeSetContext) {
    AwsLambdaInfrastructure bean = AwsLambdaInfrastructure.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AwsLambdaInfrastructure bean, ChangeContext<AwsLambdaInfrastructureYaml> changeContext) {
    AwsLambdaInfrastructureYaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck(format("Cloud Provider with name %s does not exist", yaml.getCloudProviderName()), cloudProvider);
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setRole(yaml.getIamRole());
    bean.setRegion(yaml.getRegion());
    bean.setSecurityGroupIds(yaml.getSecurityGroupIds());
    bean.setSubnetIds(yaml.getSubnetIds());
    bean.setVpcId(yaml.getVpcId());
    bean.setExpressions(yaml.getExpressions());
  }

  @Override
  public Class getYamlClass() {
    return AwsLambdaInfrastructureYaml.class;
  }
}
