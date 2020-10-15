package software.wings.resources;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;

import io.harness.CategoryTest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

public class ExecutionResourceTest extends CategoryTest {
  private static final AppService appService = mock(AppService.class);
  private static final AuthService authService = mock(AuthService.class);
  private static final WorkflowExecutionService workflowExecutionService = mock(WorkflowExecutionService.class);

  /**
   * The constant resources.
   */
  @ClassRule public static final ResourceTestRule resources = init(workflowExecutionService);

  private static ResourceTestRule init(WorkflowExecutionService workflowExecutionService) {
    try {
      ExecutionResource executionResource = new ExecutionResource();
      FieldUtils.writeField(executionResource, "authService", authService, true);
      FieldUtils.writeField(executionResource, "workflowExecutionService", workflowExecutionService, true);
      return ResourceTestRule.builder().instance(executionResource).build();
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testListExecutions() {
    String appId = generateUuid();

    PageResponse<Application> applicationPageResponse =
        aPageResponse().withResponse(newArrayList(anApplication().uuid(appId).build())).build();
    when(appService.list(anyObject())).thenReturn(applicationPageResponse);

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
