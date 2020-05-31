package io.harness.perpetualtask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.managerclient.ManagerClient;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoUtils;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import retrofit2.Call;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class AwsSshInstanceSyncExecutorTest extends CategoryTest {
  @Mock private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Mock private ManagerClient managerClient;
  @Mock private Call<RestResponse<Boolean>> call;
  @InjectMocks private AwsSshInstanceSyncExecutor executor = new AwsSshInstanceSyncExecutor();

  private ArgumentCaptor<AwsEc2ListInstancesResponse> captor =
      ArgumentCaptor.forClass(AwsEc2ListInstancesResponse.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(executor);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void runOnceWhenAwsCallSuccess() throws IOException {
    PerpetualTaskResponse perpetualTaskResponse;
    PerpetualTaskExecutionParams perpetualTaskParams = getPerpetualTaskParams();
    final Instance instance = new Instance();
    doReturn(Arrays.asList(instance))
        .when(ec2ServiceDelegate)
        .listEc2Instances(any(AwsConfig.class), anyList(), anyString(), anyList());
    doReturn(call).when(managerClient).publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());

    verify(ec2ServiceDelegate, Mockito.times(1))
        .listEc2Instances(any(AwsConfig.class), anyList(), Matchers.eq("us-east-1"), anyList());

    verify(managerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    AwsEc2ListInstancesResponse response = captor.getValue();
    verifySuccessResponse(instance, perpetualTaskResponse, response);

    doThrow(new RuntimeException()).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());
    verifySuccessResponse(instance, perpetualTaskResponse, response);
  }

  private void verifySuccessResponse(
      Instance instance, PerpetualTaskResponse perpetualTaskResponse, AwsEc2ListInstancesResponse response) {
    assertThat(response.getInstances()).containsExactly(instance);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(response.getErrorMessage()).isNull();

    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getResponseMessage()).contains("TASK_RUN_SUCCEEDED");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void runOnceWhenAwsCallThrows() throws IOException {
    PerpetualTaskResponse perpetualTaskResponse;
    PerpetualTaskExecutionParams perpetualTaskParams = getPerpetualTaskParams();
    final Instance instance = new Instance();
    doThrow(new RuntimeException("invalid credentials"))
        .when(ec2ServiceDelegate)
        .listEc2Instances(any(AwsConfig.class), anyList(), anyString(), anyList());
    doReturn(call).when(managerClient).publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());

    verify(ec2ServiceDelegate, Mockito.times(1))
        .listEc2Instances(any(AwsConfig.class), anyList(), Matchers.eq("us-east-1"), anyList());

    verify(managerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    AwsEc2ListInstancesResponse response = captor.getValue();
    verifyFailureResponse(perpetualTaskResponse, response);

    doThrow(new RuntimeException()).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), perpetualTaskParams, Instant.now());
    verifyFailureResponse(perpetualTaskResponse, response);
  }

  private void verifyFailureResponse(
      PerpetualTaskResponse perpetualTaskResponse, AwsEc2ListInstancesResponse response) {
    assertThat(response.getInstances()).isNull();
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(response.getErrorMessage()).contains("invalid credentials");

    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_FAILED);
    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getResponseMessage()).contains("invalid credentials");
  }

  private PerpetualTaskExecutionParams getPerpetualTaskParams() {
    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(AwsConfig.builder().accountId("accountId").build()));
    ByteString filterBytes = ByteString.copyFrom(KryoUtils.asBytes(Arrays.asList(new Filter())));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));

    AwsSshInstanceSyncPerpetualTaskParams params = AwsSshInstanceSyncPerpetualTaskParams.newBuilder()
                                                       .setAwsConfig(configBytes)
                                                       .setFilter(filterBytes)
                                                       .setEncryptedData(encryptionDetailsBytes)
                                                       .setRegion("us-east-1")
                                                       .build();
    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(params)).build();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cleanup() {
    assertThat(
        executor.cleanup(PerpetualTaskId.newBuilder().build(), PerpetualTaskExecutionParams.newBuilder().build()))
        .isFalse();
  }
}