package software.wings.resources;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.OrchestrationWorkflow.OrchestrationWorkflowBuilder.anOrchestrationWorkflow;

import org.junit.ClassRule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.RestResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

/**
 * Created by rishi on 12/28/16.
 */
public class WorkflowResourceTest extends WingsBaseTest {
  private static final WorkflowService WORKFLOW_SERVICE = mock(WorkflowService.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new WorkflowResource(WORKFLOW_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  private static String APP_ID = "APP_ID";
  private static String WORKFLOW_ID = "WORKFLOW_ID";
  private static final OrchestrationWorkflow ORCHESTRATION_WORKFLOW =
      anOrchestrationWorkflow().withAppId(APP_ID).withUuid(WORKFLOW_ID).build();

  /**
   * Should read workflow.
   */
  @Test
  public void shouldReadWorkflow() {
    when(WORKFLOW_SERVICE.readOrchestrationWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(ORCHESTRATION_WORKFLOW);

    RestResponse<OrchestrationWorkflow> restResponse =
        RESOURCES.client()
            .target(format("/workflows/%s?appId=%s", WORKFLOW_ID, APP_ID))
            .request()
            .get(new GenericType<RestResponse<OrchestrationWorkflow>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", ORCHESTRATION_WORKFLOW);
    verify(WORKFLOW_SERVICE).readOrchestrationWorkflow(APP_ID, WORKFLOW_ID);
  }
}
