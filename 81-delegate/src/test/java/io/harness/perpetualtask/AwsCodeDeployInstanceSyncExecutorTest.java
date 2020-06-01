package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.ABOSII;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams;
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
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesResponse;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.class)
public class AwsCodeDeployInstanceSyncExecutorTest extends CategoryTest {
  @Mock private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private Call<RestResponse<Boolean>> call;

  @InjectMocks @Inject private AwsCodeDeployInstanceSyncExecutor executor;

  @Before
  public void setup() throws IOException {
    doReturn(singletonList(new Instance()))
        .when(ec2ServiceDelegate)
        .listEc2Instances(
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString(), anyListOf(Filter.class));

    doReturn(retrofit2.Response.success("success")).when(call).execute();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunOnceSuccessful() throws IOException {
    PerpetualTaskExecutionParams perpetualTaskParams = getPerpetualTaskParams();
    ArgumentCaptor<AwsCodeDeployListDeploymentInstancesResponse> captor =
        ArgumentCaptor.forClass(AwsCodeDeployListDeploymentInstancesResponse.class);

    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    PerpetualTaskResponse perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());

    verify(ec2ServiceDelegate, times(1))
        .listEc2Instances(
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), eq("us-east-1"), anyListOf(Filter.class));
    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());
    verifySuccessResponse(perpetualTaskResponse, captor.getValue());

    doThrow(new RuntimeException()).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());
    verifySuccessResponse(perpetualTaskResponse, captor.getValue());
  }

  private void verifySuccessResponse(
      PerpetualTaskResponse taskResponse, AwsCodeDeployListDeploymentInstancesResponse response) {
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getInstances()).isNotEmpty();
    assertThat(response.getErrorMessage()).isNull();

    assertThat(taskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
    assertThat(taskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(taskResponse.getResponseMessage()).contains("TASK_RUN_SUCCEEDED");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testRunOnceWhenInstancesFetchThrowException() {
    PerpetualTaskExecutionParams perpetualTaskParams = getPerpetualTaskParams();
    ArgumentCaptor<AwsCodeDeployListDeploymentInstancesResponse> captor =
        ArgumentCaptor.forClass(AwsCodeDeployListDeploymentInstancesResponse.class);

    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doThrow(new InvalidRequestException("Invalid deployment id"))
        .when(ec2ServiceDelegate)
        .listEc2Instances(
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), eq("us-east-1"), anyListOf(Filter.class));
    PerpetualTaskResponse perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());

    verify(ec2ServiceDelegate, times(1))
        .listEc2Instances(
            any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString(), anyListOf(Filter.class));
    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());
    verifyFailureResponse(perpetualTaskResponse, captor.getValue());
  }

  private void verifyFailureResponse(
      PerpetualTaskResponse taskResponse, AwsCodeDeployListDeploymentInstancesResponse response) {
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(response.getInstances()).isNullOrEmpty();
    assertThat(response.getErrorMessage()).isEqualTo("Invalid deployment id");

    assertThat(taskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_FAILED);
    assertThat(taskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(taskResponse.getResponseMessage()).contains("Invalid deployment id");
  }

  private PerpetualTaskExecutionParams getPerpetualTaskParams() {
    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(AwsConfig.builder().accountId("accountId").build()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));
    AwsCodeDeployInstanceSyncPerpetualTaskParams.Builder paramsBuilder =
        AwsCodeDeployInstanceSyncPerpetualTaskParams.newBuilder();

    paramsBuilder.setRegion("us-east-1");
    paramsBuilder.setAwsConfig(configBytes);
    paramsBuilder.setEncryptedData(encryptionDetailsBytes);
    ByteString filterBytes = ByteString.copyFrom(KryoUtils.asBytes(singletonList(new Filter())));
    paramsBuilder.setFilter(filterBytes);

    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(paramsBuilder.build())).build();
  }
}