package software.wings.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
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

    assertEquals(infrastructureMapping.getClass(), AwsAmiInfrastructureMapping.class);

    AwsAmiInfrastructureMapping infraMapping = (AwsAmiInfrastructureMapping) infrastructureMapping;

    assertEquals(infraMapping.getRegion(), REGION);
    assertEquals(infraMapping.getClassicLoadBalancers(), CLASSIC_LOAD_BALANCERS);
    assertEquals(infraMapping.getHostNameConvention(), HOSTNAME_CONVENTION);
    assertEquals(infraMapping.getAutoScalingGroupName(), WingsTestConstants.AUTO_SCALING_GROUP_NAME);
    assertEquals(infraMapping.getComputeProviderSettingId(), WingsTestConstants.COMPUTE_PROVIDER_ID);
    assertEquals(infraMapping.getStageClassicLoadBalancers(), STAGE_CLASSIC_LOAD_BALANCERS);
    assertEquals(infraMapping.getTargetGroupArns(), TARGET_GROUP_ARNS);
    assertEquals(infraMapping.getStageTargetGroupArns(), STAGE_TARGET_GROUP_ARNS);
    assertEquals(infraMapping.getRegion(), REGION);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMappingClass() {
    Class<? extends InfrastructureMapping> mappingClass = awsAmiInfrastructure.getMappingClass();

    assertThat(mappingClass).isNotNull();
    assertEquals(mappingClass, AwsAmiInfrastructureMapping.class);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFieldMapForClass() {
    Map<String, Object> fieldMap = awsAmiInfrastructure.getFieldMapForClass();
    assertThat(fieldMap).isNotNull();

    assertThat(fieldMap.containsKey("cloudProviderId")).isFalse();

    assertThat(fieldMap.containsKey("region")).isTrue();
    assertEquals(fieldMap.get("region"), REGION);

    assertThat(fieldMap.containsKey("hostNameConvention")).isTrue();
    assertEquals(fieldMap.get("hostNameConvention"), HOSTNAME_CONVENTION);

    assertThat(fieldMap.containsKey("autoScalingGroupName")).isTrue();
    assertEquals(fieldMap.get("autoScalingGroupName"), WingsTestConstants.AUTO_SCALING_GROUP_NAME);

    assertThat(fieldMap.containsKey("region")).isTrue();
    assertEquals(fieldMap.get("region"), REGION);

    assertThat(fieldMap.containsKey("classicLoadBalancers")).isTrue();
    assertEquals(fieldMap.get("classicLoadBalancers"), CLASSIC_LOAD_BALANCERS);

    assertThat(fieldMap.containsKey("stageClassicLoadBalancers")).isTrue();
    assertEquals(fieldMap.get("stageClassicLoadBalancers"), STAGE_CLASSIC_LOAD_BALANCERS);

    assertThat(fieldMap.containsKey("targetGroupArns")).isTrue();
    assertEquals(fieldMap.get("targetGroupArns"), TARGET_GROUP_ARNS);

    assertThat(fieldMap.containsKey("stageTargetGroupArns")).isTrue();
    assertEquals(fieldMap.get("stageTargetGroupArns"), STAGE_TARGET_GROUP_ARNS);
  }
}