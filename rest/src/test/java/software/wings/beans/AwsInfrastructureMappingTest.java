package software.wings.beans;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.InvalidRequestException;

import java.util.HashMap;
import java.util.Map;

public class AwsInfrastructureMappingTest extends WingsBaseTest {
  @Test
  public void testInfrastructureMapping() {
    Map<String, Object> map = new HashMap<>();

    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping().build();

    assertThatThrownBy(() -> awsInfrastructureMapping.applyProvisionerVariables(map))
        .isInstanceOf(InvalidRequestException.class);

    map.put("region", "dummy-region");
    awsInfrastructureMapping.applyProvisionerVariables(map);
    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter()).isNull();

    map.put("vpcs", asList("dummy-vpc"));
    map.put("subnets", asList("dummy-subnets"));
    map.put("securityGroups", asList("dummy-securityGroups"));
    map.put("tags", ImmutableMap.<String, Object>of("key", "value"));

    awsInfrastructureMapping.applyProvisionerVariables(map);
    assertThat(awsInfrastructureMapping.getRegion()).isEqualTo("dummy-region");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getVpcIds()).containsExactly("dummy-vpc");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getSubnetIds()).containsExactly("dummy-subnets");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getSecurityGroupIds())
        .containsExactly("dummy-securityGroups");
    assertThat(awsInfrastructureMapping.getAwsInstanceFilter().getTags().size()).isEqualTo(1);
  }
}