package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.deployment.WorkflowVariablesMetadata;
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
import com.google.inject.Inject;
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
}
