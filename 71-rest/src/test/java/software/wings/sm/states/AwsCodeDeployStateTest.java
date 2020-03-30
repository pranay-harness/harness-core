package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.util.Map;

public class AwsCodeDeployStateTest extends WingsBaseTest {
  @InjectMocks AwsCodeDeployState awsCodeDeployState = new AwsCodeDeployState("awsCodeDeployState");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testValidateFields() {
    on(awsCodeDeployState).set("bucket", "bucketValue");
    on(awsCodeDeployState).set("key", "keyValue");
    on(awsCodeDeployState).set("bundleType", "bundleTypeValue");
    on(awsCodeDeployState).set("steadyStateTimeout", 0);
    Map<String, String> invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields).isEmpty();

    on(awsCodeDeployState).set("steadyStateTimeout", -10);
    invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields.size()).isEqualTo(1);
    assertThat(invalidFields).containsKey("steadyStateTimeout");

    on(awsCodeDeployState).set("steadyStateTimeout", 20);
    invalidFields = awsCodeDeployState.validateFields();
    assertThat(invalidFields).isEmpty();
  }
}
