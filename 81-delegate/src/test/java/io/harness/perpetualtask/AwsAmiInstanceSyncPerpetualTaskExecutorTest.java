package io.harness.perpetualtask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.DelegateTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsAmiInstanceSyncPerpetualTaskParams;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoSerializer;
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
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class AwsAmiInstanceSyncPerpetualTaskExecutorTest extends DelegateTest {
  @Mock private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;
  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock private Call<RestResponse<Boolean>> call;

  private ArgumentCaptor<AwsAsgListInstancesResponse> captor =
      ArgumentCaptor.forClass(AwsAsgListInstancesResponse.class);

  @Inject KryoSerializer kryoSerializer;

  @InjectMocks private AwsAmiInstanceSyncPerpetualTaskExecutor executor;

  @Before
  public void setup() {
    on(executor).set("kryoSerializer", kryoSerializer);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void runOnceWithAwsCallSuccess() throws IOException {
    final Instance instance = new Instance();
    doReturn(Arrays.asList(instance))
        .when(awsAsgHelperServiceDelegate)
        .listAutoScalingGroupInstances(any(AwsConfig.class), anyList(), eq("us-east-1"), eq("asg-1"));
    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    PerpetualTaskResponse perpetualTaskResponse;
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    final AwsAsgListInstancesResponse awsResponse = captor.getValue();

    verifyAwsCallSuccess(instance, perpetualTaskResponse, awsResponse);

    doThrow(new RuntimeException()).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verifyAwsCallSuccess(instance, perpetualTaskResponse, awsResponse);
  }

  private void verifyAwsCallSuccess(
      Instance instance, PerpetualTaskResponse perpetualTaskResponse, AwsAsgListInstancesResponse awsResponse) {
    assertThat(awsResponse.getErrorMessage()).isNull();
    assertThat(awsResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(awsResponse.getAsgName()).isEqualTo("asg-1");
    assertThat(awsResponse.getInstances()).containsExactly(instance);

    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo("TASK_RUN_SUCCEEDED");
    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_SUCCEEDED);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void runOnceWithAwsCallFailure() throws IOException {
    doThrow(new RuntimeException("exception message"))
        .when(awsAsgHelperServiceDelegate)
        .listAutoScalingGroupInstances(any(AwsConfig.class), anyList(), eq("us-east-1"), eq("asg-1"));
    doReturn(call)
        .when(delegateAgentManagerClient)
        .publishInstanceSyncResult(anyString(), anyString(), any(ResponseData.class));
    doReturn(retrofit2.Response.success("success")).when(call).execute();

    PerpetualTaskResponse perpetualTaskResponse;
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    verify(delegateAgentManagerClient, times(1)).publishInstanceSyncResult(eq("id"), eq("accountId"), captor.capture());

    final AwsAsgListInstancesResponse awsResponse = captor.getValue();

    veifyAwsCallFailure(perpetualTaskResponse, awsResponse);

    doThrow(new RuntimeException()).when(call).execute();
    perpetualTaskResponse =
        executor.runOnce(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams(), Instant.now());

    veifyAwsCallFailure(perpetualTaskResponse, awsResponse);
  }

  private void veifyAwsCallFailure(
      PerpetualTaskResponse perpetualTaskResponse, AwsAsgListInstancesResponse awsResponse) {
    assertThat(awsResponse.getErrorMessage()).isEqualTo("exception message");
    assertThat(awsResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(awsResponse.getAsgName()).isEqualTo("asg-1");
    assertThat(awsResponse.getInstances()).isNull();

    assertThat(perpetualTaskResponse.getResponseMessage()).isEqualTo("exception message");
    assertThat(perpetualTaskResponse.getResponseCode()).isEqualTo(Response.SC_OK);
    assertThat(perpetualTaskResponse.getPerpetualTaskState()).isEqualTo(PerpetualTaskState.TASK_RUN_FAILED);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void cleanup() {
    assertThat(executor.cleanup(PerpetualTaskId.newBuilder().setId("id").build(), getPerpetualTaskParams())).isFalse();
  }

  private PerpetualTaskExecutionParams getPerpetualTaskParams() {
    ByteString configBytes = ByteString.copyFrom(KryoUtils.asBytes(AwsConfig.builder().accountId("accountId").build()));
    ByteString encryptionDetailsBytes = ByteString.copyFrom(KryoUtils.asBytes(new ArrayList<>()));

    AwsAmiInstanceSyncPerpetualTaskParams params = AwsAmiInstanceSyncPerpetualTaskParams.newBuilder()
                                                       .setAwsConfig(configBytes)
                                                       .setEncryptedData(encryptionDetailsBytes)
                                                       .setRegion("us-east-1")
                                                       .setAsgName("asg-1")
                                                       .build();
    return PerpetualTaskExecutionParams.newBuilder().setCustomizedParams(Any.pack(params)).build();
  }
}