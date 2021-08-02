package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAMARTH;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.delegate.TaskSelector;
import io.harness.logging.CommandExecutionStatus;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.plan.execution.SetupAbstractionKeys;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class StepUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void shouldTestGenerateLogAbstractionsBasicFunctionality() {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setMetadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("pipelineId").setRunSequence(1).build())
            .putAllSetupAbstractions(setupAbstractions)
            .addLevels(Level.newBuilder().setIdentifier("runStep1").setGroup("group1").build())
            .build();

    LinkedHashMap<String, String> expectedLogAbstractionMap = new LinkedHashMap<>();
    expectedLogAbstractionMap.put("accountId", "accountId");
    expectedLogAbstractionMap.put("projectId", "projectId");
    expectedLogAbstractionMap.put("orgId", "orgId");
    expectedLogAbstractionMap.put("pipelineId", "pipelineId");
    expectedLogAbstractionMap.put("runSequence", "1");
    expectedLogAbstractionMap.put("level0", "runStep1");

    assertThat(StepUtils.generateLogAbstractions(ambiance)).isEqualTo(expectedLogAbstractionMap);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void shouldTestGenerateLogAbstractionsLastGroupFunctionality() {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setMetadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("pipelineId").setRunSequence(1).build())
            .putAllSetupAbstractions(setupAbstractions)
            .addLevels(Level.newBuilder().setIdentifier("runStep1").setGroup("group1").build())
            .addLevels(Level.newBuilder().setIdentifier("runStep2").setGroup("group2").build())
            .build();

    LinkedHashMap<String, String> expectedLogAbstractionMap = new LinkedHashMap<>();
    expectedLogAbstractionMap.put("accountId", "accountId");
    expectedLogAbstractionMap.put("projectId", "projectId");
    expectedLogAbstractionMap.put("orgId", "orgId");
    expectedLogAbstractionMap.put("pipelineId", "pipelineId");
    expectedLogAbstractionMap.put("runSequence", "1");
    expectedLogAbstractionMap.put("level0", "runStep1");

    assertThat(StepUtils.generateLogAbstractions(ambiance, "group1")).isEqualTo(expectedLogAbstractionMap);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void shouldTestGenerateLogAbstractionsStepRetryFunctionality() {
    HashMap<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "accountId");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "projectId");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "orgId");
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setMetadata(ExecutionMetadata.newBuilder().setPipelineIdentifier("pipelineId").setRunSequence(1).build())
            .putAllSetupAbstractions(setupAbstractions)
            .addLevels(Level.newBuilder().setIdentifier("runStep1").setGroup("group1").build())
            .addLevels(Level.newBuilder().setIdentifier("runStep2").setGroup("group2").setRetryIndex(2).build())
            .build();

    LinkedHashMap<String, String> expectedLogAbstractionMap = new LinkedHashMap<>();
    expectedLogAbstractionMap.put("accountId", "accountId");
    expectedLogAbstractionMap.put("projectId", "projectId");
    expectedLogAbstractionMap.put("orgId", "orgId");
    expectedLogAbstractionMap.put("pipelineId", "pipelineId");
    expectedLogAbstractionMap.put("runSequence", "1");
    expectedLogAbstractionMap.put("level0", "runStep1");
    expectedLogAbstractionMap.put("level1", "runStep2_2");

    assertThat(StepUtils.generateLogAbstractions(ambiance)).isEqualTo(expectedLogAbstractionMap);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetStepStatus() {
    assertThat(StepUtils.getStepStatus(null)).isEqualTo(null);

    for (CommandExecutionStatus commandExecutionStatus : CommandExecutionStatus.values()) {
      // asserts that all values are registered with the method, else exception should be thrown
      assertThat(StepUtils.getStepStatus(commandExecutionStatus)).isNotNull();
    }
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetTaskSelectors() {
    assertThat(StepUtils.getTaskSelectors(null)).isEmpty();

    ParameterField<List<String>> listParameterField = new ParameterField<>();
    assertThat(StepUtils.getTaskSelectors(listParameterField)).isEmpty();

    listParameterField = ParameterField.createExpressionField(true, "<+input>", null, true);
    assertThat(StepUtils.getTaskSelectors(listParameterField)).isEmpty();

    listParameterField = ParameterField.createValueField(Collections.singletonList("abc"));
    List<TaskSelector> taskSelectors = StepUtils.getTaskSelectors(listParameterField);
    assertThat(taskSelectors).hasSize(1);
    assertThat(taskSelectors.get(0).getSelector()).isEqualTo("abc");

    listParameterField = ParameterField.createValueField(Arrays.asList("s1", "s2"));
    taskSelectors = StepUtils.getTaskSelectors(listParameterField);
    assertThat(taskSelectors).hasSize(2);
    assertThat(taskSelectors.get(0).getSelector()).isEqualTo("s1");
    assertThat(taskSelectors.get(1).getSelector()).isEqualTo("s2");
  }
}
