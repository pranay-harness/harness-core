package software.wings.infra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    assertNotNull(mappingClass);
    assertEquals(mappingClass, AwsAmiInfrastructureMapping.class);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetFieldMapForClass() {
    Map<String, Object> fieldMap = awsAmiInfrastructure.getFieldMapForClass();
    assertNotNull(fieldMap);

    assertFalse(fieldMap.containsKey("cloudProviderId"));

    assertTrue(fieldMap.containsKey("region"));
    assertEquals(fieldMap.get("region"), REGION);

    assertTrue(fieldMap.containsKey("hostNameConvention"));
    assertEquals(fieldMap.get("hostNameConvention"), HOSTNAME_CONVENTION);

    assertTrue(fieldMap.containsKey("autoScalingGroupName"));
    assertEquals(fieldMap.get("autoScalingGroupName"), WingsTestConstants.AUTO_SCALING_GROUP_NAME);

    assertTrue(fieldMap.containsKey("region"));
    assertEquals(fieldMap.get("region"), REGION);

    assertTrue(fieldMap.containsKey("classicLoadBalancers"));
    assertEquals(fieldMap.get("classicLoadBalancers"), CLASSIC_LOAD_BALANCERS);

    assertTrue(fieldMap.containsKey("stageClassicLoadBalancers"));
    assertEquals(fieldMap.get("stageClassicLoadBalancers"), STAGE_CLASSIC_LOAD_BALANCERS);

    assertTrue(fieldMap.containsKey("targetGroupArns"));
    assertEquals(fieldMap.get("targetGroupArns"), TARGET_GROUP_ARNS);

    assertTrue(fieldMap.containsKey("stageTargetGroupArns"));
    assertEquals(fieldMap.get("stageTargetGroupArns"), STAGE_TARGET_GROUP_ARNS);
  }
}