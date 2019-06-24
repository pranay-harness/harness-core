package software.wings.beans;

import static software.wings.beans.AwsAmiInfrastructureMapping.AwsAmiInfrastructureMappingKeys;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;

public class AwsAmiInfrastructureMappingTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    AwsAmiInfrastructureMapping infrastructureMapping = new AwsAmiInfrastructureMapping();
    Map<String, Object> resolvedBlueprints = new HashMap<>();
    Assertions.assertThatThrownBy(() -> infrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put(AwsAmiInfrastructureMappingKeys.region, "fda");
    resolvedBlueprints.put(AwsAmiInfrastructureMappingKeys.autoScalingGroupName, "fda");
    infrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null, true);

    resolvedBlueprints.remove(AwsAmiInfrastructureMappingKeys.autoScalingGroupName);
    Assertions
        .assertThatThrownBy(
            () -> new AwsAmiInfrastructureMapping().applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);

    resolvedBlueprints.put(AwsAmiInfrastructureMappingKeys.autoScalingGroupName, "fda");
    resolvedBlueprints.remove(AwsAmiInfrastructureMappingKeys.region);
    Assertions.assertThatThrownBy(() -> infrastructureMapping.applyProvisionerVariables(resolvedBlueprints, null, true))
        .isInstanceOf(InvalidRequestException.class);
  }
}
