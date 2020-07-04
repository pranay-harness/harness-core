package io.harness.ng.core.remote.client.rest.factory;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.SendTaskRequest;
import io.harness.delegate.SendTaskResponse;
import io.harness.delegate.TaskId;
import io.harness.grpc.ng.NgDelegateTaskGrpcClient;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.NGDelegateClientResource;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class NGDelegateClientResourceTest extends BaseTest {
  @Mock NgDelegateTaskGrpcClient ngDelegateServiceGrpcClient;
  private NGDelegateClientResource ngSecretManagerResource;

  @Before
  public void doSetup() {
    ngSecretManagerResource = new NGDelegateClientResource(ngDelegateServiceGrpcClient);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testCreate() {
    String taskId = "test";
    SendTaskResponse sendTaskResponse =
        SendTaskResponse.newBuilder().setTaskId(TaskId.newBuilder().setId(taskId).build()).build();

    when(ngDelegateServiceGrpcClient.sendTask(any(SendTaskRequest.class))).thenReturn(sendTaskResponse);
    String returnedTaskId = ngSecretManagerResource.create();
    assertThat(returnedTaskId).isNotNull();
    assertThat(returnedTaskId).isEqualTo(taskId);
  }
}
