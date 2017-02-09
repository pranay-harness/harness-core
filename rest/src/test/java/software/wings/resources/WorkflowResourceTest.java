package software.wings.resources;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.OrchestrationWorkflow.OrchestrationWorkflowBuilder.anOrchestrationWorkflow;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageResponse.Builder.aPageResponse;

import com.google.common.collect.Lists;

import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import software.wings.WingsBaseTest;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.RestResponse;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.JsonUtils;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * Created by rishi on 12/28/16.
 */
public class WorkflowResourceTest extends WingsBaseTest {
  private static final WorkflowService WORKFLOW_SERVICE = mock(WorkflowService.class);

  @Captor private ArgumentCaptor<PageRequest<OrchestrationWorkflow>> pageRequestArgumentCaptor;

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
   * Should create workflow.
   */
  @Test
  public void shouldCreateWorkflow() {
    OrchestrationWorkflow orchestrationWorkflow2 =
        anOrchestrationWorkflow().withAppId(APP_ID).withUuid(UUIDGenerator.getUuid()).build();
    when(WORKFLOW_SERVICE.createOrchestrationWorkflow(ORCHESTRATION_WORKFLOW)).thenReturn(orchestrationWorkflow2);

    RestResponse<OrchestrationWorkflow> restResponse =
        RESOURCES.client()
            .target(format("/workflows?appId=%s", APP_ID))
            .request()
            .post(Entity.entity(ORCHESTRATION_WORKFLOW, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<OrchestrationWorkflow>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", orchestrationWorkflow2);
    verify(WORKFLOW_SERVICE).createOrchestrationWorkflow(ORCHESTRATION_WORKFLOW);
  }

  /**
   * Should list workflows.
   */
  @Test
  public void shouldListWorkflow() {
    PageRequest<OrchestrationWorkflow> pageRequest = aPageRequest().build();
    PageResponse<OrchestrationWorkflow> pageResponse =
        aPageResponse().withResponse(Lists.newArrayList(ORCHESTRATION_WORKFLOW)).build();
    when(WORKFLOW_SERVICE.listOrchestrationWorkflows(any(PageRequest.class), any(Integer.class)))
        .thenReturn(pageResponse);

    RestResponse<PageResponse<OrchestrationWorkflow>> restResponse =
        RESOURCES.client()
            .target(format("/workflows?appId=%s&previousExecutionsCount=2", APP_ID))
            .request()
            .get(new GenericType<RestResponse<PageResponse<OrchestrationWorkflow>>>() {});

    log().info(JsonUtils.asJson(restResponse));
    verify(WORKFLOW_SERVICE).listOrchestrationWorkflows(pageRequestArgumentCaptor.capture(), eq(2));
    assertThat(pageRequestArgumentCaptor.getValue()).isNotNull();
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", pageResponse);
  }

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
