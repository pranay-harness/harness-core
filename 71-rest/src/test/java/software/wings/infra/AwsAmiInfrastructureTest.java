package software.wings.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.infra.InfraDefinitionTestConstants.CLASSIC_LOAD_BALANCERS;
import static software.wings.infra.InfraDefinitionTestConstants.HOSTNAME_CONVENTION;
import static software.wings.infra.InfraDefinitionTestConstants.REGION;
import static software.wings.infra.InfraDefinitionTestConstants.STAGE_CLASSIC_LOAD_BALANCERS;
import static software.wings.infra.InfraDefinitionTestConstants.STAGE_TARGET_GROUP_ARNS;
import static software.wings.infra.InfraDefinitionTestConstants.TARGET_GROUP_ARNS;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.utils.WingsTestConstants;

import java.util.Map;

public class AwsAmiInfrastructureTest {
  private final AwsAmiInfrastructure awsAmiInfrastructure =
      AwsAmiInfrastructure.builder()
          .autoScalingGroupName(WingsTestConstants.AUTO_SCALING_GROUP_NAME)
          .classicLoadBalancers(CLASSIC_LOAD_BALANCERS)
          .cloudProviderId(WingsTestConstants.COMPUTE_PROVIDER_ID)
          .hostNameConvention(HOSTNAME_CONVENTION)
          .region(REGION)
          .stageClassicLoadBalancers(STAGE_CLASSIC_LOAD_BALANCERS)
          .stageTargetGroupArns(STAGE_TARGET_GROUP_ARNS)
          .targetGroupArns(TARGET_GROUP_ARNS)
          .build();

  @Test
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    InfrastructureMapping infrastructureMapping = awsAmiInfrastructure.getInfraMapping();

    assertThat(AwsAmiInfrastructureMapping.class).isEqualTo(infrastructureMapping.getClass());

    AwsAmiInfrastructureMapping infraMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;

    assertThat(REGION).isEqualTo(infraMapping.getRegion());
    assertThat(CLASSIC_LOAD_BALANCERS).isEqualTo(infraMapping.getClassicLoadBalancers());
    assertThat(HOSTNAME_CONVENTION).isEqualTo(infraMapping.getHostNameConvention());
    assertThat(WingsTestConstants.AUTO_SCALING_GROUP_NAME).isEqualTo(infraMapping.getAutoScalingGroupName());
    assertThat(WingsTestConstants.COMPUTE_PROVIDER_ID).isEqualTo(infraMapping.getComputeProviderSettingId());
    assertThat(STAGE_CLASSIC_LOAD_BALANCERS).isEqualTo(infraMapping.getStageClassicLoadBalancers());
    assertThat(TARGET_GROUP_ARNS).isEqualTo(infraMapping.getTargetGroupArns());
    assertThat(STAGE_TARGET_GROUP_ARNS).isEqualTo(infraMapping.getStageTargetGroupArns());
    assertThat(REGION).isEqualTo(infraMapping.getRegion());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMappingClass() {
    Class<? extends InfrastructureMapping> mappingClass = awsAmiInfrastructure.getMappingClass();

    assertThat(mappingClass).isNotNull();
    assertThat(AwsAmiInfrastructureMapping.class).isEqualTo(mappingClass);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFieldMapForClass() {
    Map<String, Object> fieldMap = awsAmiInfrastructure.getFieldMapForClass();
    assertThat(fieldMap).isNotNull();

    assertThat(fieldMap.containsKey("cloudProviderId")).isFalse();

    assertThat(fieldMap.containsKey("region")).isTrue();
    assertThat(REGION).isEqualTo(fieldMap.get("region"));

    assertThat(fieldMap.containsKey("hostNameConvention")).isTrue();
    assertThat(HOSTNAME_CONVENTION).isEqualTo(fieldMap.get("hostNameConvention"));

    assertThat(fieldMap.containsKey("autoScalingGroupName")).isTrue();
    assertThat(WingsTestConstants.AUTO_SCALING_GROUP_NAME).isEqualTo(fieldMap.get("autoScalingGroupName"));

    assertThat(fieldMap.containsKey("region")).isTrue();
    assertThat(REGION).isEqualTo(fieldMap.get("region"));

    assertThat(fieldMap.containsKey("classicLoadBalancers")).isTrue();
    assertThat(CLASSIC_LOAD_BALANCERS).isEqualTo(fieldMap.get("classicLoadBalancers"));

    assertThat(fieldMap.containsKey("stageClassicLoadBalancers")).isTrue();
    assertThat(STAGE_CLASSIC_LOAD_BALANCERS).isEqualTo(fieldMap.get("stageClassicLoadBalancers"));

    assertThat(fieldMap.containsKey("targetGroupArns")).isTrue();
    assertThat(TARGET_GROUP_ARNS).isEqualTo(fieldMap.get("targetGroupArns"));

    assertThat(fieldMap.containsKey("stageTargetGroupArns")).isTrue();
    assertThat(STAGE_TARGET_GROUP_ARNS).isEqualTo(fieldMap.get("stageTargetGroupArns"));
  }
}