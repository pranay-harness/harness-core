package io.harness.cdng.environment.steps;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class EnvironmentStepTest extends CDNGBaseTest {
  @Inject EnvironmentService environmentService;
  @Inject EnvironmentStep environmentStep;

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
                                          .identifier("test-id")
                                          .type(EnvironmentType.PreProduction)
                                          .tags(Collections.emptyList())
                                          .build();
    EnvironmentStepParameters stepParameters =
        EnvironmentStepParameters.builder().environment(environmentYaml).environmentOverrides(null).build();

    StepResponse stepResponse = environmentStep.executeSync(ambiance, stepParameters, null, null);

    assertThat(((List<StepResponse.StepOutcome>) stepResponse.getStepOutcomes()).get(0).getOutcome())
        .isEqualTo(environmentYaml);

    Optional<Environment> savedEnvironment = environmentService.get("accountId", "orgId", "projectId", "test-id");
    assertThat(savedEnvironment).isPresent();
  }
}