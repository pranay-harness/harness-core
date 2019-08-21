package software.wings.service.intfc.aws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import com.amazonaws.services.applicationautoscaling.model.PredefinedMetricSpecification;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ScalingPolicy;
import com.amazonaws.services.applicationautoscaling.model.TargetTrackingScalingPolicyConfiguration;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;

import java.util.List;

public class AwsAppAutoScalingHelperServiceDelegateTest extends WingsBaseTest {
  @Inject private AwsAppAutoScalingHelperServiceDelegate scalingHelperServiceDelegate;

  @Test
  @Category(UnitTests.class)
  public void testGetJsonForAwsScalableTarget() throws Exception {
    String json = "{\n"
        + "            \"ServiceNamespace\": \"ecs\",\n"
        + "            \"ScalableDimension\": \"ecs:service:DesiredCount\",\n"
        + "            \"MinCapacity\": 2,\n"
        + "            \"MaxCapacity\": 5,\n"
        + "\"RoleARN\":\"RollARN\"\n"
        + "        }";

    ScalableTarget scalableTarget = scalingHelperServiceDelegate.getScalableTargetFromJson(json);
    assertThat(scalableTarget).isNotNull();
    assertEquals("ecs", scalableTarget.getServiceNamespace());
    assertEquals("ecs:service:DesiredCount", scalableTarget.getScalableDimension());
    assertEquals(2, scalableTarget.getMinCapacity().intValue());
    assertEquals(5, scalableTarget.getMaxCapacity().intValue());
    assertEquals("RollARN", scalableTarget.getRoleARN());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetJsonForAwsScalingPolicy() throws Exception {
    String json = "{\n"
        + "            \"ScalableDimension\": \"ecs:service:DesiredCount\",\n"
        + "            \"ServiceNamespace\": \"ecs\",\n"
        + "            \"PolicyName\": \"TrackingPolicyTest\",\n"
        + "            \"PolicyType\": \"TargetTrackingScaling\",\n"
        + "            \"TargetTrackingScalingPolicyConfiguration\": {\n"
        + "                \"TargetValue\": 60.0,\n"
        + "                \"PredefinedMetricSpecification\": {\n"
        + "                    \"PredefinedMetricType\": \"ECSServiceAverageCPUUtilization\"\n"
        + "                },\n"
        + "                \"ScaleOutCooldown\": 300,\n"
        + "                \"ScaleInCooldown\": 300\n"
        + "            }"
        + "        }";

    List<ScalingPolicy> scalingPolicies = scalingHelperServiceDelegate.getScalingPolicyFromJson(json);
    assertThat(scalingPolicies).isNotNull();
    assertEquals(1, scalingPolicies.size());

    ScalingPolicy scalingPolicy = scalingPolicies.get(0);
    validateScalingPolicy("TrackingPolicyTest", "ECSServiceAverageCPUUtilization", scalingPolicy);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetJsonForAwsScalingPolicies() throws Exception {
    String json = "  [ {\n"
        + "            \"ScalableDimension\": \"ecs:service:DesiredCount\",\n"
        + "            \"ServiceNamespace\": \"ecs\",\n"
        + "            \"PolicyName\": \"TrackingPolicyTest\",\n"
        + "            \"PolicyType\": \"TargetTrackingScaling\",\n"
        + "            \"TargetTrackingScalingPolicyConfiguration\": {\n"
        + "                \"TargetValue\": 60.0,\n"
        + "                \"PredefinedMetricSpecification\": {\n"
        + "                    \"PredefinedMetricType\": \"ECSServiceAverageCPUUtilization\"\n"
        + "                },\n"
        + "                \"ScaleOutCooldown\": 300,\n"
        + "                \"ScaleInCooldown\": 300\n"
        + "            }"
        + "        },{"
        + "            \"ScalableDimension\": \"ecs:service:DesiredCount\",\n"
        + "            \"ServiceNamespace\": \"ecs\",\n"
        + "            \"PolicyName\": \"TrackingPolicyTest2\",\n"
        + "            \"PolicyType\": \"TargetTrackingScaling\",\n"
        + "            \"TargetTrackingScalingPolicyConfiguration\": {\n"
        + "                \"TargetValue\": 60.0,\n"
        + "                \"PredefinedMetricSpecification\": {\n"
        + "                    \"PredefinedMetricType\": \"ECSServiceAverageMemoryUtilization\"\n"
        + "                },\n"
        + "                \"ScaleOutCooldown\": 300,\n"
        + "                \"ScaleInCooldown\": 300\n"
        + "            }"
        + "        }]  ";

    List<ScalingPolicy> scalingPolicies = scalingHelperServiceDelegate.getScalingPolicyFromJson(json);
    assertThat(scalingPolicies).isNotNull();
    assertEquals(2, scalingPolicies.size());

    ScalingPolicy scalingPolicy = scalingPolicies.get(0);
    validateScalingPolicy("TrackingPolicyTest", "ECSServiceAverageCPUUtilization", scalingPolicy);

    scalingPolicy = scalingPolicies.get(1);
    validateScalingPolicy("TrackingPolicyTest2", "ECSServiceAverageMemoryUtilization", scalingPolicy);
  }

  private void validateScalingPolicy(String name, String predefinedMetricType, ScalingPolicy scalingPolicy) {
    assertEquals(name, scalingPolicy.getPolicyName());
    assertEquals("ecs", scalingPolicy.getServiceNamespace());
    assertEquals("ecs:service:DesiredCount", scalingPolicy.getScalableDimension());

    assertThat(scalingPolicy.getTargetTrackingScalingPolicyConfiguration()).isNotNull();
    TargetTrackingScalingPolicyConfiguration configuration =
        scalingPolicy.getTargetTrackingScalingPolicyConfiguration();

    assertEquals(60, configuration.getTargetValue().intValue());
    assertEquals(300, configuration.getScaleInCooldown().intValue());
    assertEquals(300, configuration.getScaleOutCooldown().intValue());

    assertThat(configuration.getPredefinedMetricSpecification()).isNotNull();
    PredefinedMetricSpecification metricSpecification = configuration.getPredefinedMetricSpecification();
    assertEquals(predefinedMetricType, metricSpecification.getPredefinedMetricType());
  }
}
