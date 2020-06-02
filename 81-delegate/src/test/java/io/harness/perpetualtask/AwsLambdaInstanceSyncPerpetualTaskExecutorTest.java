package io.harness.perpetualtask;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoUtils;
import org.eclipse.jetty.server.Response;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsMetricsResponse;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class AwsLambdaInstanceSyncPerpetualTaskExecutorTest extends CategoryTest {
  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private AwsLambdaHelperServiceDelegate awsLambdaHelperServiceDelegate;
  @Mock private AwsCloudWatchHelperServiceDelegate awsCloudWatchHelperServiceDelegate;
  @Mock private Call<RestResponse<Boolean>> call;

  private ArgumentCaptor<AwsLambdaDetailsMetricsResponse> captor =
      ArgumentCaptor.forClass(AwsLambdaDetailsMetricsResponse.class);

  @InjectMocks
  private AwsLambdaInstanceSyncPerpetualTaskExecutor executor = new AwsLambdaInstanceSyncPerpetualTaskExecutor();

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void runOnceWithAwsCallSuccess() throws IOException {
    doReturn(AwsLambdaDetailsResponse.builder().executionStatus(SUCCESS).build())
        .when(awsLambdaHelperServiceDelegate)
        .getFunctionDetails(any(AwsLambdaDetailsRequest.class));

    doReturn(AwsCloudWatchStatisticsResponse.builder().executionStatus(SUCCESS).build())
        .when(awsCloudWatchHelperServiceDelegate)
        .getMetricStatistics(any(AwsCloudWatchStatisticsRequest.class));

    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    PerpetualTaskResponse perpetualTaskResponse;
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    final AwsLambdaDetailsMetricsResponse awsResponse = captor.getValue();

    verifyAwsCall(perpetualTaskResponse, awsResponse, SUCCESS, PerpetualTaskState.TASK_RUN_SUCCEEDED.name(),
        PerpetualTaskState.TASK_RUN_SUCCEEDED);

    doThrow(new RuntimeException("Failed to publish execution result")).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verifyAwsCall(perpetualTaskResponse, awsResponse, SUCCESS, PerpetualTaskState.TASK_RUN_SUCCEEDED.name(),
        PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void runOnceWithAwsCallSuccessAndCloudWatchCallFailure() throws IOException {
    doReturn(AwsLambdaDetailsResponse.builder().executionStatus(SUCCESS).build())
        .when(awsLambdaHelperServiceDelegate)
        .getFunctionDetails(any(AwsLambdaDetailsRequest.class));

    doThrow(new RuntimeException("Failed to get cloudwatch statistics"))
        .when(awsCloudWatchHelperServiceDelegate)
        .getMetricStatistics(any(AwsCloudWatchStatisticsRequest.class));

    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    PerpetualTaskResponse perpetualTaskResponse;
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    final AwsLambdaDetailsMetricsResponse awsResponse = captor.getValue();

    verifyAwsCall(perpetualTaskResponse, awsResponse, SUCCESS, PerpetualTaskState.TASK_RUN_SUCCEEDED.name(),
        PerpetualTaskState.TASK_RUN_SUCCEEDED);

    doThrow(new RuntimeException("Failed to publish execution result")).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verifyAwsCall(perpetualTaskResponse, awsResponse, SUCCESS, PerpetualTaskState.TASK_RUN_SUCCEEDED.name(),
        PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void runOnceWithAwsCallFailure() throws IOException {
    doThrow(new RuntimeException("Failed to execute lambda function"))
        .when(awsLambdaHelperServiceDelegate)
        .getFunctionDetails(any(AwsLambdaDetailsRequest.class));

    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    PerpetualTaskResponse perpetualTaskResponse;
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    final AwsLambdaDetailsMetricsResponse awsResponse = captor.getValue();

    verifyAwsCall(perpetualTaskResponse, awsResponse, FAILED, "Failed to execute lambda function",
        PerpetualTaskState.TASK_RUN_FAILED);

    doThrow(new RuntimeException("Failed to publish execution result")).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verifyAwsCall(perpetualTaskResponse, awsResponse, FAILED, "Failed to execute lambda function",
        PerpetualTaskState.TASK_RUN_FAILED);
  }

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void cleanup() {
    assertThat(executor.cleanup(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams())).isFalse();
  }

  private void verifyAwsCall(PerpetualTaskResponse perpetualTaskResponse, AwsLambdaDetailsMetricsResponse awsResponse,
      ExecutionStatus executionStatus, String message, PerpetualTaskState perpetualTaskState) {
    if (PerpetualTaskState.TASK_RUN_SUCCEEDED.name().equals(message)) {
      assertThat(awsResponse.getErrorMessage()).isNull();
    } else {
      assertThat(awsResponse.getErrorMessage()).isEqualTo(message);
    }
    assertThat(awsResponse.getExecutionStatus()).isEqualTo(executionStatus);

    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo(message);
    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(perpetualTaskState);
  }

  private PerpetualTaskExecutionParams getPerpetualTaskParams() {
    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(AwsConfig.builder().accountId("accountId").build()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));

    AwsLambdaInstanceSyncPerpetualTaskParams params = AwsLambdaInstanceSyncPerpetualTaskParams.newBuilder()
                                                          .setAwsConfig(configBytes)
                                                          .setEncryptedData(encryptionDetailsBytes)
                                                          .setRegion("us-east-1")
                                                          .setFunctionName("function-1")
                                                          .setQualifier("version-1")
                                                          .build();
    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(params)).build();
  }
}
