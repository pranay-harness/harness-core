package software.wings.graphql.datafetcher.pipeline;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.DeploymentMetadata;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.RuntimeInputExecutionInputsController;
import software.wings.graphql.schema.mutation.execution.input.QLVariableInput;
import software.wings.graphql.schema.query.QLExecutionInputsToResumePipelineQueryParams;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.ArtifactType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@OwnedBy(HarnessTeam.CDC)
public class RuntimeInputExecutionInputsControllerTest extends WingsBaseTest {
  @Mock ServiceResourceService serviceResourceService;
  @Mock PipelineExecutionController pipelineExecutionController;
  @Mock PipelineService pipelineService;
  @Mock AuthHandler authHandler;
  @Mock WorkflowExecutionService workflowExecutionService;

  private static ObjectMapper mapper;

  @Before
  public void beforeTest() {
    mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }
  @Inject @InjectMocks RuntimeInputExecutionInputsController controller = new RuntimeInputExecutionInputsController();

  @Test
  @Owner(developers = {DEEPAK_PUTHRAYA})
  @Category(UnitTests.class)
  public void testQLExecutionInputReturnsCorrectServices() {
    WorkflowExecution pipelineExecution = jsonToObj("./execution/pipeline_execution.json", WorkflowExecution.class);
    when(workflowExecutionService.getWorkflowExecution(anyString(), anyString())).thenReturn(pipelineExecution);
    Pipeline pipeline = jsonToObj("./pipeline/pipeline.json", Pipeline.class);
    when(pipelineService.readPipeline(anyString(), anyString(), eq(true))).thenReturn(pipeline);
    when(pipelineExecutionController.resolveEnvId(eq(pipeline), anyListOf(QLVariableInput.class))).thenReturn("envId");
    when(pipelineExecutionController.validateAndResolvePipelineVariables(
             eq(pipeline), anyListOf(QLVariableInput.class), eq("envId"), eq(new ArrayList<>()), eq(false)))
        .thenReturn(new HashMap<>());

    DeploymentMetadata metadata =
        DeploymentMetadata.builder().artifactRequiredServiceIds(Lists.newArrayList("SERVICE_ID")).build();
    PageResponse<Service> response = new PageResponse<>();
    response.add(Service.builder()
                     .uuid("serviceId")
                     .name("serviceName")
                     .appId("appId")
                     .description("description")
                     .artifactType(ArtifactType.DOCKER)
                     .deploymentType(DeploymentType.HELM)
                     .createdAt(0)
                     .createdBy(null)
                     .build());
    when(pipelineService.fetchDeploymentMetadata(anyString(), eq(pipeline), eq(new HashMap<>()))).thenReturn(metadata);
    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(false), eq(false), anyString()))
        .thenReturn(response);

    JsonNode actual = toJson(controller.fetch(QLExecutionInputsToResumePipelineQueryParams.builder()
                                                  .applicationId("appId")
                                                  .pipelineStageElementId("stageElementId")
                                                  .pipelineExecutionId("executionId")
                                                  .variableInputs(new ArrayList<>())
                                                  .build(),
        "accountId"));
    JsonNode expected = jsonToObj("./execution/qlExecution_input_expected.json", JsonNode.class);
    assertEquals("QLInputs should be equal", actual.toString(), expected.toString());
  }

  private static <T> T jsonToObj(String filPath, Class<T> tClass) {
    File file =
        new File(RuntimeInputExecutionInputsControllerTest.class.getClassLoader().getResource(filPath).getFile());
    try {
      return mapper.readValue(file, tClass);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static JsonNode toJson(Object obj) {
    return mapper.convertValue(obj, JsonNode.class);
  }
}
