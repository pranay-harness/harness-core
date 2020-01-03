package software.wings.annotation;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.ContainerInfrastructureMapping.ContainerInfrastructureMappingKeys;
import software.wings.beans.EcsInfrastructureMapping;

import java.util.HashMap;
import java.util.Map;

public class BlueprintProcessorTest extends WingsBaseTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testValidateKeys() {
    AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
    Map<String, Object> blueprints = new HashMap<>();
    BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, blueprints);
    blueprints.put("abc", "def");
    Map<String, Object> finalBlueprints = blueprints;
    assertThatThrownBy(() -> BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, finalBlueprints))
        .isInstanceOf(InvalidRequestException.class);
    blueprints.remove("abc");
    blueprints.put("region", "def");
    BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, blueprints);

    // Test for super class fields
    EcsInfrastructureMapping ecsInfrastructureMapping = new EcsInfrastructureMapping();
    blueprints = new HashMap<>();
    blueprints.put(ContainerInfrastructureMappingKeys.clusterName, "abc");
    BlueprintProcessor.validateKeys(ecsInfrastructureMapping, blueprints);
  }
}
