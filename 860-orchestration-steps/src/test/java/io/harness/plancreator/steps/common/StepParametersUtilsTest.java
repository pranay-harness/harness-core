package io.harness.plancreator.steps.common;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class StepParametersUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetStepParameters() {
    StepElementConfig stepElementConfig = StepElementConfig.builder()
                                              .identifier("IDENTIFIER")
                                              .name("NAME")
                                              .description("DESCRIPTION")
                                              .type("TYPE")
                                              .skipCondition(ParameterField.createValueField("SKIPCONDITION"))
                                              .when(ParameterField.createValueField("WHEN"))
                                              .build();
    StepElementParameters stepElementParameters = StepParametersUtils.getStepParameters(stepElementConfig).build();
    assertThat(stepElementParameters.getIdentifier()).isEqualTo(stepElementConfig.getIdentifier());
    assertThat(stepElementParameters.getName()).isEqualTo(stepElementConfig.getName());
    assertThat(stepElementParameters.getDescription()).isEqualTo(stepElementConfig.getDescription());
    assertThat(stepElementParameters.getType()).isEqualTo(stepElementConfig.getType());
    assertThat(stepElementParameters.getSkipCondition()).isEqualTo(stepElementConfig.getSkipCondition());
    assertThat(stepElementParameters.getWhen()).isEqualTo(stepElementConfig.getWhen());
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetStageParameters() {
    StageElementConfig config = StageElementConfig.builder()
                                    .identifier("IDENTIFIER")
                                    .name("NAME")
                                    .description(ParameterField.createValueField("DESCRIPTION"))
                                    .type("TYPE")
                                    .skipCondition(ParameterField.createValueField("SKIPCONDITION"))
                                    .when(ParameterField.createValueField("WHEN"))
                                    .build();
    StageElementParameters stageParameters = StepParametersUtils.getStageParameters(config).build();
    assertThat(stageParameters.getIdentifier()).isEqualTo(config.getIdentifier());
    assertThat(stageParameters.getName()).isEqualTo(config.getName());
    assertThat(stageParameters.getDescription()).isEqualTo(config.getDescription());
    assertThat(stageParameters.getType()).isEqualTo(config.getType());
    assertThat(stageParameters.getSkipCondition()).isEqualTo(config.getSkipCondition());
    assertThat(stageParameters.getWhen()).isEqualTo(config.getWhen());
  }
}