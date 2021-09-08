package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.VED;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.workflow.StepYaml;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class BarrierStepYamlBuilderTest extends WingsBaseTest {
  @Inject private BarrierStepYamlBuilder barrierStepYamlBuilder;

  @Test
  @Owner(developers = VED)
  @Category(UnitTests.class)
  public void testValidate() {
    Map<String, Object> inputProperties = getInputProperties();
    ChangeContext changeContext = buildChangeContext(inputProperties);

    assertThatThrownBy(() -> barrierStepYamlBuilder.validate(changeContext))
        .hasMessage("Barrier step Identifier cannot be empty")
        .isInstanceOf(InvalidRequestException.class);
  }

  private Map<String, Object> getInputProperties() {
    Map<String, Object> inputProperties = new HashMap<>();
    inputProperties.put("identifier", null);
    inputProperties.put("timeoutMillis", 60000);

    return inputProperties;
  }

  private static ChangeContext buildChangeContext(Map<String, Object> inputProperties) {
    return ChangeContext.Builder.aChangeContext()
        .withYaml(StepYaml.builder().properties(inputProperties).build())
        .withChange(Change.Builder.aFileChange().withAccountId(WingsTestConstants.ACCOUNT_ID).build())
        .build();
  }
}
