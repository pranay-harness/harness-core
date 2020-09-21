package io.harness.cdng.environment.steps;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EnvironmentStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EnvironmentService environmentService;
  @InjectMocks EnvironmentStep environmentStep;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testExecuteSync() {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance = Ambiance.builder().setupAbstractions(setupAbstractions).build();

    EnvironmentYaml environmentYaml = EnvironmentYaml.builder()
                                          .identifier(ParameterField.createValueField("test-id"))
                                          .name(ParameterField.createValueField("test-id"))
                                          .type(EnvironmentType.PreProduction)
                                          .tags(Collections.emptyList())
                                          .build();
    EnvironmentStepParameters stepParameters =
        EnvironmentStepParameters.builder().environment(environmentYaml).environmentOverrides(null).build();

    Environment expectedEnv = Environment.builder()
                                  .identifier("test-id")
                                  .type(EnvironmentType.PreProduction)
                                  .tags(Collections.emptyList())
                                  .build();

    EnvironmentOutcome environmentOutcome = EnvironmentOutcome.builder()
                                                .identifier("test-id")
                                                .name("test-id")
                                                .description("")
                                                .type(EnvironmentType.PreProduction)
                                                .tags(Collections.emptyList())
                                                .build();
    doReturn(expectedEnv).when(environmentService).upsert(expectedEnv);

    StepResponse stepResponse = environmentStep.executeSync(ambiance, stepParameters, null, null);

    assertThat(((List<StepResponse.StepOutcome>) stepResponse.getStepOutcomes()).get(0).getOutcome())
        .isEqualTo(environmentOutcome);
  }
}