/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.GARVIT;

import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.workflow.StepSkipStrategy.Scope.ALL_STEPS;
import static software.wings.beans.workflow.StepSkipStrategy.Scope.SPECIFIC_STEPS;
import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStepType;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class StepSkipStrategyYamlHandlerTest extends WingsBaseTest {
  private static final String ASSERTION_EXPRESSION = "true";

  @InjectMocks @Inject private StepSkipStrategyYamlHandler stepSkipStrategyYamlHandler;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testToYaml() {
    StepSkipStrategy stepSkipStrategy =
        new StepSkipStrategy(StepSkipStrategy.Scope.ALL_STEPS, null, ASSERTION_EXPRESSION);
    StepSkipStrategy.Yaml yaml = stepSkipStrategyYamlHandler.toYaml(stepSkipStrategy, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getScope()).isEqualTo(ALL_STEPS.name());
    assertThat(yaml.getSteps()).isNull();
    assertThat(yaml.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    stepSkipStrategy = new StepSkipStrategy(SPECIFIC_STEPS, null, ASSERTION_EXPRESSION);
    yaml = stepSkipStrategyYamlHandler.toYaml(stepSkipStrategy, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getScope()).isEqualTo(SPECIFIC_STEPS.name());
    assertThat(yaml.getSteps()).isNullOrEmpty();
    assertThat(yaml.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    stepSkipStrategy = new StepSkipStrategy(SPECIFIC_STEPS, Collections.emptyList(), ASSERTION_EXPRESSION);
    yaml = stepSkipStrategyYamlHandler.toYaml(stepSkipStrategy, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getScope()).isEqualTo(SPECIFIC_STEPS.name());
    assertThat(yaml.getSteps()).isNullOrEmpty();
    assertThat(yaml.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    String stepId = "stepId";
    String stepName = "stepName";
    PhaseStep phaseStep =
        aPhaseStep(PhaseStepType.ENABLE_SERVICE).addStep(GraphNode.builder().id(stepId).name(stepName).build()).build();

    stepSkipStrategy = new StepSkipStrategy(SPECIFIC_STEPS, Collections.singletonList(stepId), ASSERTION_EXPRESSION);
    stepSkipStrategy.setPhaseStep(phaseStep);
    yaml = stepSkipStrategyYamlHandler.toYaml(stepSkipStrategy, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getScope()).isEqualTo(SPECIFIC_STEPS.name());
    assertThat(yaml.getSteps()).containsExactly(stepName);
    assertThat(yaml.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testToBean() {
    StepSkipStrategy.Yaml yaml = StepSkipStrategy.Yaml.builder()
                                     .scope(ALL_STEPS.name())
                                     .steps(Collections.singletonList("randomStepId"))
                                     .assertionExpression(ASSERTION_EXPRESSION)
                                     .build();
    ChangeContext changeContext = ChangeContext.Builder.aChangeContext()
                                      .withYamlType(YamlType.STEP_SKIP_STRATEGY)
                                      .withYaml(yaml)
                                      .withChange(GitFileChange.Builder.aGitFileChange()
                                                      .withFilePath("Setup/Applications/APP/Workflows/WORKFLOW.yaml")
                                                      .build())
                                      .build();
    StepSkipStrategy stepSkipStrategy = stepSkipStrategyYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(stepSkipStrategy).isNotNull();
    assertThat(stepSkipStrategy.getScope()).isEqualTo(ALL_STEPS);
    assertThat(stepSkipStrategy.getStepIds()).isNullOrEmpty();
    assertThat(stepSkipStrategy.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    yaml = StepSkipStrategy.Yaml.builder()
               .scope(SPECIFIC_STEPS.name())
               .steps(null)
               .assertionExpression(ASSERTION_EXPRESSION)
               .build();
    changeContext.setYaml(yaml);
    stepSkipStrategy = stepSkipStrategyYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(stepSkipStrategy).isNotNull();
    assertThat(stepSkipStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(stepSkipStrategy.getStepIds()).isNullOrEmpty();
    assertThat(stepSkipStrategy.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    yaml = StepSkipStrategy.Yaml.builder()
               .scope(SPECIFIC_STEPS.name())
               .steps(Collections.emptyList())
               .assertionExpression(ASSERTION_EXPRESSION)
               .build();
    changeContext.setYaml(yaml);
    stepSkipStrategy = stepSkipStrategyYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(stepSkipStrategy).isNotNull();
    assertThat(stepSkipStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(stepSkipStrategy.getStepIds()).isNullOrEmpty();
    assertThat(stepSkipStrategy.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);

    String stepId = "stepId";
    String stepName = "stepName";
    yaml = StepSkipStrategy.Yaml.builder()
               .scope(SPECIFIC_STEPS.name())
               .steps(Collections.singletonList(stepName))
               .assertionExpression(ASSERTION_EXPRESSION)
               .build();
    changeContext.setYaml(yaml);
    try {
      // This should throw an exception as phase step is not set in change context.
      stepSkipStrategyYamlHandler.upsertFromYaml(changeContext, null);
      assertThat(true).isFalse();
    } catch (Exception ignored) {
    }

    PhaseStep phaseStep =
        aPhaseStep(PhaseStepType.ENABLE_SERVICE).addStep(GraphNode.builder().id(stepId).name(stepName).build()).build();
    changeContext.setProperties(Collections.singletonMap(PhaseStepYamlHandler.PHASE_STEP_PROPERTY_NAME, phaseStep));
    stepSkipStrategy = stepSkipStrategyYamlHandler.upsertFromYaml(changeContext, null);
    assertThat(stepSkipStrategy).isNotNull();
    assertThat(stepSkipStrategy.getScope()).isEqualTo(SPECIFIC_STEPS);
    assertThat(stepSkipStrategy.getStepIds()).containsExactly(stepId);
    assertThat(stepSkipStrategy.getAssertionExpression()).isEqualTo(ASSERTION_EXPRESSION);
  }
}
