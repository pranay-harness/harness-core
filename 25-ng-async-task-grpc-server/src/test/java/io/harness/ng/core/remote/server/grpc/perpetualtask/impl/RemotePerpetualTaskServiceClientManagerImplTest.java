package io.harness.ng.core.remote.server.grpc.perpetualtask.impl;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import com.google.protobuf.Message;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskExecutionResponse;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.RemotePerpetualTaskClientContext;
import io.harness.perpetualtask.example.SamplePerpetualTaskParams;
import io.harness.perpetualtask.remote.RemotePerpetualTaskServiceClient;
import io.harness.perpetualtask.remote.RemotePerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.remote.ValidationTaskDetails;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import javax.validation.constraints.NotNull;

public class RemotePerpetualTaskServiceClientManagerImplTest extends CategoryTest {
  public static final String ACCOUNT_ID = "accountId";
  @Mock RemotePerpetualTaskServiceClient perpetualTaskServiceClient;
  @Spy RemotePerpetualTaskServiceClientRegistry registry = new RemotePerpetualTaskServiceClientRegistry();
  @InjectMocks private RemotePerpetualTaskServiceClientManagerImpl manager;
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    registry.registerClient(getTaskType(), perpetualTaskServiceClient);
  }

  @NotNull
  private String getTaskType() {
    return "sample_task";
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getValidationTask() {
    final ValidationTaskDetails validationTaskDetails = ValidationTaskDetails.builder().build();
    doReturn(validationTaskDetails)
        .when(perpetualTaskServiceClient)
        .getValidationTask(any(RemotePerpetualTaskClientContext.class), anyString());
    final ValidationTaskDetails validationTask = manager.getValidationTask(getTaskType(), getContext(), ACCOUNT_ID);

    assertThat(validationTask).isEqualTo(validationTaskDetails);
  }

  @NotNull
  private RemotePerpetualTaskClientContext getContext() {
    return RemotePerpetualTaskClientContext.newBuilder().build();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void getTaskParams() {
    final SamplePerpetualTaskParams taskParams1 = SamplePerpetualTaskParams.newBuilder().build();
    doReturn(taskParams1).when(perpetualTaskServiceClient).getTaskParams(any());
    final Message taskParams = manager.getTaskParams(getTaskType(), getContext());
    assertThat(taskParams).isEqualTo(taskParams1);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void reportPerpetualTaskStateChange() {
    final PerpetualTaskExecutionResponse newResponse =
        PerpetualTaskExecutionResponse.newBuilder().setTaskState("TASK_ASSIGNED").build();
    final PerpetualTaskExecutionResponse oldResponse =
        PerpetualTaskExecutionResponse.newBuilder().setTaskState("TASK_ASSIGNED").build();
    manager.reportPerpetualTaskStateChange("id", getTaskType(), newResponse, oldResponse);
    Mockito.verify(perpetualTaskServiceClient, times(1))
        .onTaskStateChange(eq("id"), any(PerpetualTaskResponse.class), any(PerpetualTaskResponse.class));
  }
}