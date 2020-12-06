package software.wings.service.impl.pipeline;

import static io.harness.rule.OwnerRule.POOJA;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RuntimeInputsConfig;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.sm.StateType;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PipelineServiceHelperTest extends WingsBaseTest {
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdateLoopingInfoWhenLooped() {
    List<Variable> userVariables = new ArrayList<>();
    Variable infraVar1 = aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra1").build();
    userVariables.add(infraVar1);
    OrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build();
    Workflow workflow = aWorkflow().orchestrationWorkflow(orchestrationWorkflow).build();
    Map<String, String> workflowVariable = ImmutableMap.of("infra1", "testVal1,testVal2");
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().workflowVariables(workflowVariable).name("test step").type("ENV_STATE").build();

    PipelineStage pipelineStage = PipelineStage.builder().pipelineStageElements(asList(pipelineStageElement)).build();
    List<String> infraDefIds = new ArrayList<>();
    PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, false);
    assertThat(pipelineStage.isLooped()).isEqualTo(true);
    assertThat(pipelineStage.getLoopedVarName()).isEqualTo("infra1");
    assertThat(infraDefIds.size()).isEqualTo(2);
    assertThat(infraDefIds.contains("testVal1")).isTrue();
    assertThat(infraDefIds.contains("testVal2")).isTrue();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdateLoopingInfoWhenLoopedRuntimeInfra() {
    List<Variable> userVariables = new ArrayList<>();
    Variable infraVar1 = aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra1").build();
    userVariables.add(infraVar1);
    OrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build();
    Workflow workflow = aWorkflow().orchestrationWorkflow(orchestrationWorkflow).build();
    Map<String, String> workflowVariable = ImmutableMap.of("infra1", "${infraVal}");
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder()
            .workflowVariables(workflowVariable)
            .runtimeInputsConfig(RuntimeInputsConfig.builder().runtimeInputVariables(asList("infra1")).build())
            .name("test step")
            .type("ENV_STATE")
            .build();

    PipelineStage pipelineStage = PipelineStage.builder().pipelineStageElements(asList(pipelineStageElement)).build();
    List<String> infraDefIds = new ArrayList<>();
    PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, true);
    assertThat(pipelineStage.isLooped()).isEqualTo(true);
    assertThat(pipelineStage.getLoopedVarName()).isEqualTo("infra1");
    assertThat(infraDefIds.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdateLoopingInfoWhenNotLooped() {
    List<Variable> userVariables = new ArrayList<>();
    OrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build();
    Workflow workflow = aWorkflow().orchestrationWorkflow(orchestrationWorkflow).build();
    Map<String, String> workflowVariable = ImmutableMap.of("infra1", "testVal1,testVal2");
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().workflowVariables(workflowVariable).name("test step").type("ENV_STATE").build();

    PipelineStage pipelineStage = PipelineStage.builder().pipelineStageElements(asList(pipelineStageElement)).build();
    List<String> infraDefIds = new ArrayList<>();
    PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, false);
    assertThat(pipelineStage.isLooped()).isEqualTo(false);
    assertThat(pipelineStage.getLoopedVarName()).isEqualTo(null);
    assertThat(infraDefIds.size()).isEqualTo(0);

    Variable envVar1 = aVariable().entityType(ENVIRONMENT).name("env1").build();
    userVariables.add(envVar1);

    PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, false);
    assertThat(pipelineStage.isLooped()).isEqualTo(false);
    assertThat(pipelineStage.getLoopedVarName()).isEqualTo(null);
    assertThat(infraDefIds.size()).isEqualTo(0);

    Variable infraVar1 = aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra1").build();
    Variable infraVar2 = aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra2").build();

    userVariables.add(infraVar1);
    userVariables.add(infraVar2);

    PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, false);
    assertThat(pipelineStage.isLooped()).isEqualTo(false);
    assertThat(pipelineStage.getLoopedVarName()).isEqualTo(null);
    assertThat(infraDefIds.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdateLoopingInfoWhenNotLoopedInvalid() {
    List<Variable> userVariables = new ArrayList<>();
    Variable infraVar1 = aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra1").build();
    userVariables.add(infraVar1);
    OrchestrationWorkflow orchestrationWorkflow =
        aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build();
    Workflow workflow = aWorkflow().orchestrationWorkflow(orchestrationWorkflow).build();
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().workflowVariables(new HashMap<>()).name("test step").type("ENV_STATE").build();

    PipelineStage pipelineStage = PipelineStage.builder().pipelineStageElements(asList(pipelineStageElement)).build();
    List<String> infraDefIds = new ArrayList<>();

    assertThatThrownBy(() -> PipelineServiceHelper.updateLoopingInfo(pipelineStage, workflow, infraDefIds, false))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("No value supplied in pipeline for infra variable: infra1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdatePipelineWithLoopedState() {
    Map<String, String> workflowVariable = ImmutableMap.of("infra1", "testVal1,testVal2");
    PipelineStageElement pipelineStageElement = PipelineStageElement.builder()
                                                    .workflowVariables(workflowVariable)
                                                    .properties(new HashMap<>())
                                                    .name("test step")
                                                    .type("ENV_STATE")
                                                    .build();
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .pipelineStageElements(asList(pipelineStageElement))
                                      .loopedVarName("infra1")
                                      .looped(true)
                                      .build();
    Pipeline pipeline = Pipeline.builder().name("Test pipeline").pipelineStages(asList(pipelineStage)).build();
    PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, false);
    assertThat(pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getType())
        .isEqualTo(StateType.ENV_LOOP_STATE.getType());
    Map<String, Object> properties =
        pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getProperties();
    assertThat(properties.get("loopedValues")).isNotNull();
    List<String> values = (List<String>) properties.get("loopedValues");
    assertThat(values.size()).isEqualTo(2);
    assertThat(values.contains("testVal1")).isTrue();
    assertThat(values.contains("testVal2")).isTrue();
    assertThat(properties.get("loopedVarName")).isEqualTo("infra1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdatePipelineWithLoopedStateRuntimeVariableNoValue() {
    Map<String, String> workflowVariable = ImmutableMap.of("infra1", "testVal1");
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder()
            .workflowVariables(workflowVariable)
            .properties(new HashMap<>())
            .runtimeInputsConfig(RuntimeInputsConfig.builder().runtimeInputVariables(asList("infra1")).build())
            .name("test step")
            .type("ENV_STATE")
            .build();
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .pipelineStageElements(asList(pipelineStageElement))
                                      .loopedVarName("infra1")
                                      .looped(true)
                                      .build();
    Pipeline pipeline = Pipeline.builder().name("Test pipeline").pipelineStages(asList(pipelineStage)).build();
    PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, true);
    assertThat(pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getType())
        .isEqualTo(StateType.ENV_LOOP_STATE.getType());
    Map<String, Object> properties =
        pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getProperties();
    assertThat(properties.get("loopedValues")).isNotNull();
    List<String> values = (List<String>) properties.get("loopedValues");
    assertThat(values.size()).isEqualTo(1);
    assertThat(values.contains("testVal1")).isTrue();
    assertThat(properties.get("loopedVarName")).isEqualTo("infra1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdatePipelineWithLoopedStateRuntimeVariableDefaultValue() {
    Map<String, String> workflowVariable = ImmutableMap.of();
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder()
            .workflowVariables(workflowVariable)
            .properties(new HashMap<>())
            .runtimeInputsConfig(RuntimeInputsConfig.builder().runtimeInputVariables(asList("infra1")).build())
            .name("test step")
            .type("ENV_STATE")
            .build();
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .pipelineStageElements(asList(pipelineStageElement))
                                      .loopedVarName("infra1")
                                      .looped(true)
                                      .build();
    Pipeline pipeline = Pipeline.builder().name("Test pipeline").pipelineStages(asList(pipelineStage)).build();
    PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, true);
    assertThat(pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getType())
        .isEqualTo(StateType.ENV_LOOP_STATE.getType());
    Map<String, Object> properties =
        pipeline.getPipelineStages().get(0).getPipelineStageElements().get(0).getProperties();
    assertThat(properties.get("loopedValues")).isNotNull();
    List<String> values = (List<String>) properties.get("loopedValues");
    assertThat(values.size()).isEqualTo(0);
    assertThat(properties.get("loopedVarName")).isEqualTo("infra1");
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdatePipelineWithLoopedStateInvalid() {
    PipelineStageElement pipelineStageElement =
        PipelineStageElement.builder().properties(new HashMap<>()).name("test step").type("ENV_STATE").build();
    PipelineStage pipelineStage = PipelineStage.builder()
                                      .pipelineStageElements(asList(pipelineStageElement))
                                      .loopedVarName("infra1")
                                      .looped(true)
                                      .build();
    Pipeline pipeline = Pipeline.builder().name("Test pipeline").pipelineStages(asList(pipelineStage)).build();
    assertThatThrownBy(() -> PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, false))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Pipeline stage marked as loop, but doesnt have looping config");

    Map<String, String> workflowVariable = ImmutableMap.of("infra2", "testVal1,testVal2");
    pipelineStageElement.setWorkflowVariables(workflowVariable);

    assertThatThrownBy(() -> PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, false))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Pipeline stage marked as loop, but doesnt have looping config");

    workflowVariable = ImmutableMap.of("infra1", "testVal1");
    pipelineStageElement.setWorkflowVariables(workflowVariable);

    assertThatThrownBy(() -> PipelineServiceHelper.updatePipelineWithLoopedState(pipeline, false))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Pipeline stage marked as loop, but doesnt have looping config");
  }
}
