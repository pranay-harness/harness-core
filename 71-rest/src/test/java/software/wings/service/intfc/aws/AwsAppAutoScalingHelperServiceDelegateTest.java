package software.wings.service.intfc.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import com.amazonaws.services.applicationautoscaling.model.PredefinedMetricSpecification;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ScalingPolicy;
import com.amazonaws.services.applicationautoscaling.model.TargetTrackingScalingPolicyConfiguration;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;

import java.util.List;

public class AwsAppAutoScalingHelperServiceDelegateTest extends WingsBaseTest {
  @Inject private AwsAppAutoScalingHelperServiceDelegate scalingHelperServiceDelegate;

  @Test
  public void testGetJsonForAwsScalableTarget() throws Exception {
    String json = "{\n"
        + "            \"ServiceNamespace\": \"ecs\",\n"
        + "            \"ScalableDimension\": \"ecs:service:DesiredCount\",\n"
        + "            \"MinCapacity\": 2,\n"
        + "            \"MaxCapacity\": 5,\n"
        + "\"RoleARN\":\"RollARN\"\n"
        + "        }";

    ScalableTarget scalableTarget = scalingHelperServiceDelegate.getScalableTargetFromJson(json);
    assertNotNull(scalableTarget);
    assertEquals("ecs", scalableTarget.getServiceNamespace());
    assertEquals("ecs:service:DesiredCount", scalableTarget.getScalableDimension());
    assertEquals(2, scalableTarget.getMinCapacity().intValue());
    assertEquals(5, scalableTarget.getMaxCapacity().intValue());
    assertEquals("RollARN", scalableTarget.getRoleARN());
  }

  @Test
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
    assertNotNull(scalingPolicies);
    assertEquals(1, scalingPolicies.size());

    ScalingPolicy scalingPolicy = scalingPolicies.get(0);
    validateScalingPolicy("TrackingPolicyTest", "ECSServiceAverageCPUUtilization", scalingPolicy);
  }

  @Test
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
    assertNotNull(scalingPolicies);
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

    assertNotNull(scalingPolicy.getTargetTrackingScalingPolicyConfiguration());
    TargetTrackingScalingPolicyConfiguration configuration =
        scalingPolicy.getTargetTrackingScalingPolicyConfiguration();

    assertEquals(60, configuration.getTargetValue().intValue());
    assertEquals(300, configuration.getScaleInCooldown().intValue());
    assertEquals(300, configuration.getScaleOutCooldown().intValue());

    assertNotNull(configuration.getPredefinedMetricSpecification());
    PredefinedMetricSpecification metricSpecification = configuration.getPredefinedMetricSpecification();
    assertEquals(predefinedMetricType, metricSpecification.getPredefinedMetricType());
  }
}
