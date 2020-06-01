package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.ABOSII;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstListElastigroupInstancesResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstSyncTaskHandler;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Instant;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class SpotinstAmiInstanceSyncDelegateExecutorTest extends CategoryTest {
  @Mock private EncryptionService encryptionService;
  @Mock private SpotInstSyncTaskHandler taskHandler;
  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private Call<RestResponse<Boolean>> call;

  @InjectMocks @Inject private SpotinstAmiInstanceSyncDelegateExecutor executor;

  @Before
  public void setUp() {
    when(delegateAgentManagerClient.publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class)))
        .thenReturn(call);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunOnceSuccessful() {
    ArgumentCaptor<SpotInstTaskExecutionResponse> argumentCaptor =
        ArgumentCaptor.forClass(SpotInstTaskExecutionResponse.class);

    when(taskHandler.executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class)))
        .thenReturn(
            SpotInstTaskExecutionResponse.builder()
                .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                .spotInstTaskResponse(SpotInstListElastigroupInstancesResponse.builder()
                                          .elastigroupId("elastigroup-id")
                                          .elastigroupInstances(asList(new Instance().withInstanceId("ec-instance-0"),
                                              new Instance().withInstanceId("ec-instance-1")))
                                          .build())
                .build());
    PerpetualTaskResponse perpetualTaskResponse = executor.runOnce(
        PerpetualTaskId.newBuilder().setId("task-id").build(), getPerpetualTaskParams(), Instant.now());

    verify(encryptionService, times(2)).decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class));
    verify(taskHandler, times(1))
        .executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class));
    verify(delegateAgentManagerClient, times(1))
        .publishInstanceSyncResult(eq("task-id"), eq("accountId"), argumentCaptor.capture());
    SpotInstTaskExecutionResponse sentTaskExecutionResponse = argumentCaptor.getValue();

    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo("TASK_RUN_SUCCEEDED");
    assertThat(sentTaskExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(sentTaskExecutionResponse.getSpotInstTaskResponse())
        .isInstanceOf(SpotInstListElastigroupInstancesResponse.class);
    SpotInstListElastigroupInstancesResponse sentInstancesResponse =
        (SpotInstListElastigroupInstancesResponse) sentTaskExecutionResponse.getSpotInstTaskResponse();
    assertThat(sentInstancesResponse.getElastigroupId()).isEqualTo("elastigroup-id");
    assertThat(sentInstancesResponse.getElastigroupInstances().stream().map(Instance::getInstanceId))
        .containsExactlyInAnyOrder("ec-instance-0", "ec-instance-1");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunOnceFailure() {
    ArgumentCaptor<SpotInstTaskExecutionResponse> argumentCaptor =
        ArgumentCaptor.forClass(SpotInstTaskExecutionResponse.class);

    when(taskHandler.executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class)))
        .thenReturn(SpotInstTaskExecutionResponse.builder()
                        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                        .errorMessage("Unable to list instances")
                        .build());
    PerpetualTaskResponse perpetualTaskResponse = executor.runOnce(
        PerpetualTaskId.newBuilder().setId("task-id").build(), getPerpetualTaskParams(), Instant.now());

    verify(encryptionService, times(2)).decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class));
    verify(taskHandler, times(1))
        .executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class));
    verify(delegateAgentManagerClient, times(1))
        .publishInstanceSyncResult(eq("task-id"), eq("accountId"), argumentCaptor.capture());
    SpotInstTaskExecutionResponse sentTaskExecutionResponse = argumentCaptor.getValue();

    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_FAILED);
    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo("Unable to list instances");
    assertThat(sentTaskExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(sentTaskExecutionResponse.getErrorMessage()).isEqualTo("Unable to list instances");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunOnceFailureHandlerThrownException() {
    ArgumentCaptor<SpotInstTaskExecutionResponse> argumentCaptor =
        ArgumentCaptor.forClass(SpotInstTaskExecutionResponse.class);

    when(taskHandler.executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class)))
        .thenThrow(new InvalidRequestException("Unable to fetch instance list"));

    PerpetualTaskResponse perpetualTaskResponse = executor.runOnce(
        PerpetualTaskId.newBuilder().setId("task-id").build(), getPerpetualTaskParams(), Instant.now());

    verify(encryptionService, times(2)).decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class));
    verify(taskHandler, times(1))
        .executeTask(any(SpotInstTaskParameters.class), any(SpotInstConfig.class), any(AwsConfig.class));
    verify(delegateAgentManagerClient, times(1))
        .publishInstanceSyncResult(eq("task-id"), eq("accountId"), argumentCaptor.capture());
    SpotInstTaskExecutionResponse sentTaskExecutionResponse = argumentCaptor.getValue();

    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_FAILED);
    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo("Unable to fetch instance list");
    assertThat(sentTaskExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(sentTaskExecutionResponse.getErrorMessage()).isEqualTo("Unable to fetch instance list");
  }

  private PerpetualTaskExecutionParams getPerpetualTaskParams() {
    ByteString awsConfigBytes =
        ByteString.copyFrom(KryoUtils.asBytes(AwsConfig.builder().accountId("accountId").build()));
    ByteString spotinstConfigBytes =
        ByteString.copyFrom(KryoUtils.asBytes(SpotInstConfig.builder().accountId("accountId").build()));
    ByteString awsEncryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));
    ByteString spotinstEncryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));

    SpotinstAmiInstanceSyncPerpetualTaskParams taskParams =
        SpotinstAmiInstanceSyncPerpetualTaskParams.newBuilder()
            .setElastigroupId("elastigroup-id")
            .setRegion("us-east-1a")
            .setAwsConfig(awsConfigBytes)
            .setAwsEncryptedData(awsEncryptionDetailsBytes)
            .setSpotinstConfig(spotinstConfigBytes)
            .setSpotinstEncryptedData(spotinstEncryptionDetailsBytes)
            .build();

    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(taskParams)).build();
  }
}
