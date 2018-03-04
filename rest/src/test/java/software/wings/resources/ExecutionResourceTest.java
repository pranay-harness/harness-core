package software.wings.resources;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

/**
 * Created by rishi on 12/25/16.
 */
public class ExecutionResourceTest {
  private static final AppService appService = mock(AppService.class);
  private static final WorkflowExecutionService workflowExecutionService = mock(WorkflowExecutionService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new ExecutionResource(appService, workflowExecutionService)).build();

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService, workflowExecutionService);
  }

  /**
   * Test find by name.
   */
  @Test
  @Ignore
  public void testListExecutions() {
    String appId = generateUuid();

    PageRequest<Application> applicationPageRequest = aPageRequest().build();
    PageRequest<WorkflowExecution> workflowExecutionPageRequest = aPageRequest().build();

    PageResponse<Application> applicationPageResponse =
        aPageResponse().withResponse(newArrayList(anApplication().withUuid(appId).build())).build();
    when(appService.list(anyObject(), eq(false), eq(0), eq(0))).thenReturn(applicationPageResponse);

    PageResponse<WorkflowExecution> workflowExecutionPageResponse = aPageResponse().build();
    when(workflowExecutionService.listExecutions(anyObject(), eq(true), eq(true), eq(true), eq(true)))
        .thenReturn(workflowExecutionPageResponse);

    RestResponse<PageResponse<WorkflowExecution>> actual =
        resources.client()
            .target("/executions")
            .request()
            .get(new GenericType<RestResponse<PageResponse<WorkflowExecution>>>() {});
    assertThat(actual.getResource()).isEqualTo(workflowExecutionPageResponse);
  }
}
