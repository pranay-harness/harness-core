package software.wings.beans;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_AUTOSCALING_GROUP;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_ECS_EC2;
import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;

import com.google.common.collect.ImmutableMap;

import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;

public class AwsInfrastructureMappingTest extends WingsBaseTest {
  @Test
  public void testInfrastructureMapping() {
    Map<String, Object> map = new HashMap<>();

    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping().build();

    assertThatThrownBy(() -> awsInfrastructureMapping.applyProvisionerVariables(map, AWS_INSTANCE_FILTER))
        .isInstanceOf(InvalidRequestException.class);

    map.put("region", "dummy-region");
    awsInfrastructureMapping.applyProvisionerVariables(map, AWS_INSTANCE_FILTER);
    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(awsInfrastructureMapping.isProvisionInstances()).isFalse();

    map.put("vpcs", asList("dummy-vpc"));
    map.put("subnets", asList("dummy-subnets"));
    map.put("securityGroups", asList("dummy-securityGroups"));
    map.put("tags", ImmutableMap.<String, Object>of("key", "value"));

    awsInfrastructureMapping.applyProvisionerVariables(map, AWS_INSTANCE_FILTER);
    assertThat(awsInfrastructureMapping.isProvisionInstances()).isFalse();
    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getVpcIds()).containsExactly("dummy-vpc");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getSubnetIds()).containsExactly("dummy-subnets");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getSecurityGroupIds())
        .containsExactly("dummy-securityGroups");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getTags().size()).isEqualTo(1);
  }

  @Test
  public void testASGNameAndLBName() {
    Map<String, Object> map = new HashMap<>();
    map.put("region", "dummy-region");
    map.put("autoScalingGroup", "my-group");
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping().build();
    awsInfrastructureMapping.applyProvisionerVariables(map, AWS_AUTOSCALING_GROUP);
    assertThat(awsInfrastructureMapping.isProvisionInstances()).isTrue();
    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(awsInfrastructureMapping.getAutoScalingGroupName()).isEqualTo("my-group");

    final Map<String, Object> map2 = new HashMap<>();
    final AwsInfrastructureMapping awsInfrastructureMapping2 = anAwsInfrastructureMapping().build();
    assertThatThrownBy(() -> awsInfrastructureMapping2.applyProvisionerVariables(map2, AWS_AUTOSCALING_GROUP))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  public void testChangeOfFilterType() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping().build();
    Map<String, Object> map = new HashMap<>();
    map.put("region", "dummy-region");
    awsInfrastructureMapping.applyProvisionerVariables(map, AWS_INSTANCE_FILTER);
    assertThat(awsInfrastructureMapping.getAutoScalingGroupName()).isNull();
    assertThat(awsInfrastructureMapping.isProvisionInstances()).isFalse();
    map.put("autoScalingGroup", "my-group");
    awsInfrastructureMapping.applyProvisionerVariables(map, AWS_AUTOSCALING_GROUP);
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter()).isNull();
    assertThat(awsInfrastructureMapping.isProvisionInstances()).isTrue();
  }

  @Test
  public void testEcs() {
    Map<String, Object> map = new HashMap<>();
    map.put("region", "dummy-region");
    map.put("ecsCluster", "my-cluster");
    EcsInfrastructureMapping ecsInfrastructureMapping = anEcsInfrastructureMapping().build();
    ecsInfrastructureMapping.applyProvisionerVariables(map, AWS_ECS_EC2);
    assertThat(ecsInfrastructureMapping.getLaunchType()).isEqualTo("EC2");
    assertThat(ecsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(ecsInfrastructureMapping.getClusterName()).isEqualTo("my-cluster");

    final Map<String, Object> map2 = new HashMap<>();
    final EcsInfrastructureMapping ecsInfrastructureMapping1 = anEcsInfrastructureMapping().build();
    assertThatThrownBy(() -> ecsInfrastructureMapping1.applyProvisionerVariables(map2, AWS_ECS_EC2))
        .isInstanceOf(InvalidRequestException.class);
  }
}