package software.wings.annotation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsLambdaInfraStructureMapping;

import java.util.HashMap;
import java.util.Map;

public class BlueprintProcessorTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testValidateKeys() {
    AwsLambdaInfraStructureMapping awsLambdaInfraStructureMapping = new AwsLambdaInfraStructureMapping();
    Map<String, Object> blueprints = new HashMap<>();
    BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, blueprints);
    blueprints.put("abc", "def");
    assertThatThrownBy(() -> BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, blueprints))
        .isInstanceOf(InvalidRequestException.class);
    blueprints.remove("abc");
    blueprints.put("region", "def");
    BlueprintProcessor.validateKeys(awsLambdaInfraStructureMapping, blueprints);
  }
}
