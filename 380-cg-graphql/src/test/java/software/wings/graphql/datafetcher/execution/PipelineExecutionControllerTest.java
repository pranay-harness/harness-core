package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.EntityType.ENVIRONMENT;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValue;
import software.wings.graphql.schema.mutation.execution.input.QLVariableValueType;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLPipelineExecution.QLPipelineExecutionBuilder;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDC)
public class PipelineExecutionControllerTest extends WingsBaseTest {
  @Mock AuthHandler authHandler;
  @Mock AuthService authService;
  @Mock PipelineService pipelineService;
  @Mock WorkflowExecutionService workflowExecutionService;
  @Mock EnvironmentService environmentService;
  @Mock ServiceResourceService serviceResourceService;
  @Mock InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock ExecutionController executionController;
  @Mock FeatureFlagService featureFlagService;
  @Inject @InjectMocks PipelineExecutionController pipelineExecutionController = new PipelineExecutionController();

  private static final String ENVIRONMENT_DEV_ID = "ENV_DEV_ID";

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void pipelineExecutionIsBuiltCorrectlyEvenWhenStageIsDeleted() {
    // Note: Deleted few fields from this due to issues with serialization
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/workflow_execution.json", WorkflowExecution.class);

    when(workflowExecutionService.fetchWorkflowVariables(any(), any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException());

    QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
    pipelineExecutionController.populatePipelineExecution(workflowExecution, builder);
    JsonNode actual = JsonUtils.toJsonNode(builder.build());
    JsonNode expected =
        JsonUtils.readResourceFile("./execution/qlPipeline_execution_expected_when_exception.json", JsonNode.class);
    assertEquals("QLPipeline execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void pipelineExecutionIsBuiltCorrectly() {
    // Note: Deleted few fields from this due to issues with serialization
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/workflow_execution.json", WorkflowExecution.class);

    WorkflowVariablesMetadata metadata = new WorkflowVariablesMetadata(Lists.newArrayList());
    when(workflowExecutionService.fetchWorkflowVariables(any(), any(), anyString(), anyString())).thenReturn(metadata);

    QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
    pipelineExecutionController.populatePipelineExecution(workflowExecution, builder);
    JsonNode actual = JsonUtils.toJsonNode(builder.build());
    JsonNode expected = JsonUtils.readResourceFile("./execution/qlPipeline_execution_expected.json", JsonNode.class);
    assertEquals("QLPipeline execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testRunningPipelineResolveNonRuntimeEnvVariable() throws Exception {
    // Test case for getting env variable for pipelines with non runtime env variable

    WorkflowExecution execution =
        WorkflowExecution.builder()
            .executionArgs(
                ExecutionArgs.builder()
                    .workflowVariables(ImmutableMap.<String, String>builder().put("env", ENVIRONMENT_DEV_ID).build())
                    .build())
            .build();

    Variable envNonRuntimeVar = buildVariable("env", ENVIRONMENT, false);
    Pipeline pipeline = buildPipeline(null, envNonRuntimeVar);

    String actual = pipelineExecutionController.resolveEnvId(execution, pipeline, null);
    assertThat(ENVIRONMENT_DEV_ID).isEqualTo(actual);

    envNonRuntimeVar = buildVariable("env", ENVIRONMENT, null);
    pipeline = buildPipeline(null, envNonRuntimeVar);

    actual = pipelineExecutionController.resolveEnvId(execution, pipeline, null);
    assertThat(ENVIRONMENT_DEV_ID).isEqualTo(actual);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testEnvVariableNonTemplatized() {
    Pipeline pipeline = buildPipeline(null, null);
    String actual = pipelineExecutionController.resolveEnvId(pipeline, new ArrayList<>());
    assertNull(actual);

    pipeline = buildPipeline(null);
    actual = pipelineExecutionController.resolveEnvId(pipeline, null);
    assertNull(actual);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testEnvVarTemplatisedButRuntime() {
    Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, true));
    String actual = pipelineExecutionController.resolveEnvId(pipeline, new ArrayList<>(), true);
    assertNull(actual);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testEnvTemplatizedButNotPresent() throws Exception {
    assertThatThrownBy(() -> {
      Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, false));
      pipelineExecutionController.resolveEnvId(pipeline, null, false);
    }).isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(() -> {
      Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, true));
      pipelineExecutionController.resolveEnvId(pipeline, null, false);
    }).isInstanceOf(InvalidRequestException.class);

    assertThatThrownBy(() -> {
      Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, true));
      List<QLVariableInput> variableInputs = newArrayList(QLVariableInput.builder().name("service").build());
      pipelineExecutionController.resolveEnvId(pipeline, variableInputs, false);
    }).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testEnvPresentButWithExpression() {
    Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, false));
    assertThatThrownBy(
        ()
            -> pipelineExecutionController.resolveEnvId(pipeline,
                newArrayList(QLVariableInput.builder()
                                 .name("env")
                                 .variableValue(QLVariableValue.builder().type(QLVariableValueType.EXPRESSION).build())
                                 .build()),
                true))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testInvalidEnvName() {
    when(environmentService.getEnvironmentByName(anyString(), anyString())).thenReturn(null);
    Pipeline pipeline = buildPipeline(null, buildVariable("env", ENVIRONMENT, false));
    assertThatThrownBy(
        ()
            -> pipelineExecutionController.resolveEnvId(pipeline,
                newArrayList(
                    QLVariableInput.builder()
                        .name("env")
                        .variableValue(
                            QLVariableValue.builder().type(QLVariableValueType.NAME).value("does_not_exist").build())
                        .build()),
                true))
        .isInstanceOf(GeneralException.class);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testEnvPresentCorrectly() {
    when(environmentService.getEnvironmentByName(eq("APP_ID"), eq("DEV")))
        .thenReturn(Environment.Builder.anEnvironment().uuid(ENVIRONMENT_DEV_ID).build());
    when(environmentService.get(eq("APP_ID"), eq(ENVIRONMENT_DEV_ID)))
        .thenReturn(Environment.Builder.anEnvironment().uuid(ENVIRONMENT_DEV_ID).build());
    Pipeline pipeline = buildPipeline("APP_ID", buildVariable("env", ENVIRONMENT, false));
    String actual = pipelineExecutionController.resolveEnvId(pipeline,
        newArrayList(
            QLVariableInput.builder()
                .name("env")
                .variableValue(QLVariableValue.builder().type(QLVariableValueType.ID).value(ENVIRONMENT_DEV_ID).build())
                .build()),
        true);
    assertThat(actual).isEqualTo(ENVIRONMENT_DEV_ID);

    actual = pipelineExecutionController.resolveEnvId(pipeline,
        newArrayList(QLVariableInput.builder()
                         .name("env")
                         .variableValue(QLVariableValue.builder().type(QLVariableValueType.NAME).value("DEV").build())
                         .build()),
        true);

    assertThat(actual).isEqualTo(ENVIRONMENT_DEV_ID);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void pipelineExecutionByTriggerIsBuiltCorrectly() {
    // Note: Deleted few fields from this due to issues with serialization
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/workflow_execution.json", WorkflowExecution.class);

    workflowExecution.setDeploymentTriggerId("TRIGGER_ID");

    when(workflowExecutionService.fetchWorkflowVariables(any(), any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException());

    QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
    pipelineExecutionController.populatePipelineExecution(workflowExecution, builder);
    JsonNode actual = JsonUtils.toJsonNode(builder.build());
    JsonNode expected = JsonUtils.readResourceFile("./execution/qlPipeline_execution_by_trigger.json", JsonNode.class);
    assertEquals("QLPipeline execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void pipelineExecutionByApiKeyIsBuiltCorrectly() {
    // Note: Deleted few fields from this due to issues with serialization
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/workflow_execution.json", WorkflowExecution.class);

    workflowExecution.setCreatedByType(CreatedByType.API_KEY);
    workflowExecution.setCreatedBy(EmbeddedUser.builder().name("API_KEY").uuid("KEY_ID").build());

    when(workflowExecutionService.fetchWorkflowVariables(any(), any(), anyString(), anyString()))
        .thenThrow(new IllegalStateException());

    QLPipelineExecutionBuilder builder = QLPipelineExecution.builder();
    pipelineExecutionController.populatePipelineExecution(workflowExecution, builder);
    JsonNode actual = JsonUtils.toJsonNode(builder.build());
    JsonNode expected = JsonUtils.readResourceFile("./execution/qlPipeline_execution_by_apikey.json", JsonNode.class);
    assertEquals("QLPipeline execution should be equal", expected, actual);
  }

  private Variable buildVariable(String name, EntityType entityType, Boolean isRuntime) {
    Variable variable = VariableBuilder.aVariable().entityType(entityType).name(name).build();
    variable.setRuntimeInput(isRuntime);
    return variable;
  }

  private Pipeline buildPipeline(String appId, Variable... variables) {
    Pipeline pipeline = Pipeline.builder().appId(appId).build();
    if (variables != null) {
      pipeline.setPipelineVariables(newArrayList(variables));
    }
    return pipeline;
  }
}
