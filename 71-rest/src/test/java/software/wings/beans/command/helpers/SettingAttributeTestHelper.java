package software.wings.beans.command.helpers;

import static java.util.Collections.singletonList;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.InstanceUnitType.COUNT;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme.NTLM;
import static software.wings.beans.command.EcsResizeParams.EcsResizeParamsBuilder.anEcsResizeParams;
import static software.wings.beans.command.EcsSetupParams.EcsSetupParamsBuilder.anEcsSetupParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.DOMAIN;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TASK_FAMILY;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.amazonaws.regions.Regions;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.ContainerResizeParams;
import software.wings.beans.command.ContainerSetupParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.ImageDetails;
import software.wings.utils.WingsTestConstants;

public class SettingAttributeTestHelper {
  public static SettingAttribute obtainKubernetesClusterSettingAttribute(boolean useK8sDelegate) {
    return aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.CLOUD_PROVIDER)
        .withName("kubernetesClusterConfigName")
        .withAccountId(ACCOUNT_ID)
        .withValue(obtainKubernetesClusterConfig(useK8sDelegate))
        .build();
  }

  public static SettingAttribute obtainWinrmSettingAttribute() {
    return aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withName("winrmConnectionAttr")
        .withAccountId(ACCOUNT_ID)
        .withValue(WinRmConnectionAttributes.builder()
                       .accountId(ACCOUNT_ID)
                       .authenticationScheme(NTLM)
                       .username(USER_NAME)
                       .password(PASSWORD)
                       .domain(DOMAIN)
                       .port(22)
                       .useSSL(true)
                       .skipCertChecks(true)
                       .build())
        .build();
  }

  public static SettingAttribute obtainSshSettingAttribute() {
    return aSettingAttribute()
        .withCategory(SettingAttribute.SettingCategory.SETTING)
        .withName("hostConnectionAttrs")
        .withAccountId(ACCOUNT_ID)
        .withValue(aHostConnectionAttributes()
                       .withAccessType(HostConnectionAttributes.AccessType.USER_PASSWORD)
                       .withAccountId(WingsTestConstants.ACCOUNT_ID)
                       .build())
        .build();
  }

  private static KubernetesClusterConfig obtainKubernetesClusterConfig(boolean useK8sDelegate) {
    return KubernetesClusterConfig.builder()
        .masterUrl("dummy-url")
        .username("dummy-username")
        .password("dummy-password".toCharArray())
        .accountId(ACCOUNT_ID)
        .skipValidation(false)
        .useKubernetesDelegate(useK8sDelegate)
        .build();
  }

  public static ContainerResizeParams obtainECSResizeParams() {
    return anEcsResizeParams()
        .withRegion("us-east-1")
        .withRollback(false)
        .withInstanceUnitType(COUNT)
        .withUseFixedInstances(true)
        .withInstanceCount(2)
        .withFixedInstances(2)
        .withPreviousEcsAutoScalarsAlreadyRemoved(false)
        .withPreviousAwsAutoScalarConfigs(
            singletonList(AwsAutoScalarConfig.builder().scalableTargetJson("ScalTJson").build()))
        .build();
  }

  public static ContainerSetupParams obtainECSSetupParams() {
    return anEcsSetupParams()
        .withAppName(APP_NAME)
        .withEnvName(ENV_NAME)
        .withServiceName(SERVICE_NAME)
        .withImageDetails(
            ImageDetails.builder().registryUrl("ecr").sourceName("ECR").name("todolist").tag("v1").build())
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withRegion(Regions.US_EAST_1.getName())
        .withRoleArn("roleArn")
        .withTargetGroupArn("targetGroupArn")
        .withTaskFamily(TASK_FAMILY)
        .withUseLoadBalancer(false)
        .withClusterName("cluster")
        .build();
  }
}
