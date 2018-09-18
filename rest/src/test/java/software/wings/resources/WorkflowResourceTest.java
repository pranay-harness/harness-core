package software.wings.resources;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.Lists;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import software.wings.WingsBaseTest;
import software.wings.beans.RestResponse;
import software.wings.beans.Workflow;
import software.wings.beans.stats.CloneMetadata;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.JsonUtils;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * Created by rishi on 12/28/16.
 */
public class WorkflowResourceTest extends WingsBaseTest {
  private static final WorkflowService WORKFLOW_SERVICE = mock(WorkflowService.class);

  @Captor private ArgumentCaptor<PageRequest<Workflow>> pageRequestArgumentCaptor;

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
  private static final Workflow WORKFLOW = aWorkflow()
                                               .withAppId(APP_ID)
                                               .withUuid(WORKFLOW_ID)
                                               .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                                               .build();

  /**
   * Should create workflow.
   */
  @Test
  public void shouldCreateWorkflow() {
    Workflow workflow2 = aWorkflow()
                             .withAppId(APP_ID)
                             .withUuid(generateUuid())
                             .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                             .build();
    when(WORKFLOW_SERVICE.createWorkflow(WORKFLOW)).thenReturn(workflow2);

    RestResponse<Workflow> restResponse =
        RESOURCES.client()
            .target(format("/workflows?appId=%s", APP_ID))
            .request()
            .post(entity(WORKFLOW, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Workflow>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", workflow2);
    verify(WORKFLOW_SERVICE).createWorkflow(WORKFLOW);
  }

  /**
   * Should create workflow.
   */
  @Test
  public void shouldCloneWorkflow() {
    Workflow workflow2 = aWorkflow()
                             .withAppId(APP_ID)
                             .withUuid(generateUuid())
                             .withOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build())
                             .build();
    CloneMetadata cloneMetadata = CloneMetadata.builder().workflow(WORKFLOW).build();
    when(WORKFLOW_SERVICE.cloneWorkflow(APP_ID, WORKFLOW_ID, cloneMetadata)).thenReturn(workflow2);
    RestResponse<Workflow> restResponse =
        RESOURCES.client()
            .target(format("/workflows/%s/clone?appId=%s", WORKFLOW_ID, APP_ID))
            .request()
            .post(entity(cloneMetadata, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Workflow>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", workflow2);
    verify(WORKFLOW_SERVICE).cloneWorkflow(APP_ID, WORKFLOW_ID, cloneMetadata);
  }

  /**
   * Should list workflows.
   */
  @Test
  public void shouldListWorkflow() {
    PageRequest<Workflow> pageRequest = aPageRequest().build();
    PageResponse<Workflow> pageResponse = aPageResponse().withResponse(Lists.newArrayList(WORKFLOW)).build();
    when(WORKFLOW_SERVICE.listWorkflows(any(PageRequest.class), any(Integer.class))).thenReturn(pageResponse);

    RestResponse<PageResponse<Workflow>> restResponse =
        RESOURCES.client()
            .target(format("/workflows?appId=%s&previousExecutionsCount=2", APP_ID))
            .request()
            .get(new GenericType<RestResponse<PageResponse<Workflow>>>() {});

    log().info(JsonUtils.asJson(restResponse));
    verify(WORKFLOW_SERVICE).listWorkflows(pageRequestArgumentCaptor.capture(), eq(2));
    assertThat(pageRequestArgumentCaptor.getValue()).isNotNull();
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", pageResponse);
  }

  /**
   * Should read workflow.
   */
  @Test
  public void shouldReadWorkflow() {
    when(WORKFLOW_SERVICE.readWorkflow(APP_ID, WORKFLOW_ID, null)).thenReturn(WORKFLOW);

    RestResponse<Workflow> restResponse = RESOURCES.client()
                                              .target(format("/workflows/%s?appId=%s", WORKFLOW_ID, APP_ID))
                                              .request()
                                              .get(new GenericType<RestResponse<Workflow>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", WORKFLOW);
    verify(WORKFLOW_SERVICE).readWorkflow(APP_ID, WORKFLOW_ID, null);
  }
}
