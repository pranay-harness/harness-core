package io.harness.functional.delegateservice;

import static io.harness.delegate.DelegateServiceGrpc.DelegateServiceBlockingStub;
import static io.harness.rule.OwnerRule.MARKO;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.states.HttpState.HttpStateExecutionResponse;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.category.element.FunctionalTests;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.AccountId;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateAsyncService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.threading.Poller;
import io.harness.waiter.NotifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.app.MainConfiguration;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DelegateServiceTaskApiFunctionalTest extends AbstractFunctionalTest {
  private static final String NON_EXISTING_SELECTOR = "nonExistingSelector";

  @Inject private DelegateServiceBlockingStub delegateServiceBlockingStub;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private DelegateSyncService delegateSyncService;
  @Inject private DelegateAsyncService delegateAsyncService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  public void testSyncTaskExecution() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(delegateSyncService, 0L, 2L, TimeUnit.SECONDS);

    DelegateServiceGrpcClient delegateServiceGrpcClient =
        new DelegateServiceGrpcClient(delegateServiceBlockingStub, kryoSerializer);

    DelegateCallbackToken callbackToken = delegateServiceGrpcClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("!!!custom")
                                  .setConnection(configuration.getMongoConnectionFactory().getUri())
                                  .build())
            .build());

    assertThat(callbackToken).isNotNull();
    assertThat(callbackToken.getToken()).isNotBlank();

    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .header("")
                                                .method("GET")
                                                .body("")
                                                .url("https://google.com")
                                                .socketTimeoutMillis(9000)
                                                .useProxy(false)
                                                .build();

    TaskId taskId = delegateServiceGrpcClient.submitTask(callbackToken,
        AccountId.newBuilder().setId(getAccount().getUuid()).build(), TaskSetupAbstractions.newBuilder().build(),
        TaskDetails.newBuilder()
            .setMode(TaskMode.SYNC)
            .setType(TaskType.newBuilder().setType("HTTP").build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(httpTaskParameters)))
            .setExecutionTimeout(com.google.protobuf.Duration.newBuilder().setSeconds(600).setNanos(0).build())
            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
            .build(),
        httpTaskParameters.fetchRequiredExecutionCapabilities(), null);

    ResponseData responseData =
        delegateSyncService.waitForTask(taskId.getId(), "Http Execution", Duration.ofSeconds(60));

    assertThat(responseData).isNotNull();
    HttpStateExecutionResponse executionData = (HttpStateExecutionResponse) responseData;
    assertThat(executionData).isNotNull();
    assertThat(executionData.getHttpResponseCode()).isEqualTo(200);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  public void testAsyncTaskExecution() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(delegateAsyncService, 0L, 2L, TimeUnit.SECONDS);

    DelegateServiceGrpcClient delegateServiceGrpcClient =
        new DelegateServiceGrpcClient(delegateServiceBlockingStub, kryoSerializer);

    DelegateCallbackToken callbackToken = delegateServiceGrpcClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("!!!custom")
                                  .setConnection(configuration.getMongoConnectionFactory().getUri())
                                  .build())
            .build());

    assertThat(callbackToken).isNotNull();
    assertThat(callbackToken.getToken()).isNotBlank();

    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .header("")
                                                .method("GET")
                                                .body("")
                                                .url("https://google.com")
                                                .socketTimeoutMillis(9000)
                                                .useProxy(false)
                                                .build();

    TaskId taskId = delegateServiceGrpcClient.submitTask(callbackToken,
        AccountId.newBuilder().setId(getAccount().getUuid()).build(), TaskSetupAbstractions.newBuilder().build(),
        TaskDetails.newBuilder()
            .setMode(TaskMode.ASYNC)
            .setType(TaskType.newBuilder().setType("HTTP").build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(httpTaskParameters)))
            .setExecutionTimeout(com.google.protobuf.Duration.newBuilder().setSeconds(600).setNanos(0).build())
            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
            .build(),
        httpTaskParameters.fetchRequiredExecutionCapabilities(), null);

    Poller.pollFor(Duration.ofMinutes(3), Duration.ofSeconds(5), () -> {
      NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, taskId.getId());
      return notifyResponse != null;
    });

    NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, taskId.getId());
    assertThat(notifyResponse).isNotNull();
    HttpStateExecutionResponse executionData =
        (HttpStateExecutionResponse) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
    assertThat(executionData).isNotNull();
    assertThat(executionData.getHttpResponseCode()).isEqualTo(200);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  @Ignore(
      "This can be enabled, after we add support for task selectors to D2 api, so that task expiration can be simulated")
  public void
  testAsyncTaskExecutionWithErrorResponse() {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleWithFixedDelay(delegateAsyncService, 0L, 2L, TimeUnit.SECONDS);

    DelegateServiceGrpcClient delegateServiceGrpcClient =
        new DelegateServiceGrpcClient(delegateServiceBlockingStub, kryoSerializer);

    DelegateCallbackToken callbackToken = delegateServiceGrpcClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("!!!custom")
                                  .setConnection(configuration.getMongoConnectionFactory().getUri())
                                  .build())
            .build());

    assertThat(callbackToken).isNotNull();
    assertThat(callbackToken.getToken()).isNotBlank();

    HttpTaskParameters httpTaskParameters = HttpTaskParameters.builder()
                                                .header("")
                                                .method("GET")
                                                .body("")
                                                .url("https://google.com")
                                                .socketTimeoutMillis(9000)
                                                .useProxy(false)
                                                .build();

    TaskId taskId = delegateServiceGrpcClient.submitTask(callbackToken,
        AccountId.newBuilder().setId(getAccount().getUuid()).build(), TaskSetupAbstractions.newBuilder().build(),
        TaskDetails.newBuilder()
            .setMode(TaskMode.ASYNC)
            .setType(TaskType.newBuilder().setType("HTTP").build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(httpTaskParameters)))
            .setExecutionTimeout(com.google.protobuf.Duration.newBuilder().setSeconds(30).setNanos(0).build())
            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
            .build(),
        new ArrayList<>());

    Poller.pollFor(Duration.ofMinutes(3), Duration.ofSeconds(5), () -> {
      NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, taskId.getId());
      return notifyResponse != null;
    });

    NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, taskId.getId());
    assertThat(notifyResponse).isNotNull();

    ErrorNotifyResponseData executionData =
        (ErrorNotifyResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
    assertThat(executionData).isNotNull();
    assertThat(executionData.getErrorMessage()).isNotNull();
    assertThat(executionData.getErrorMessage().contains(NON_EXISTING_SELECTOR)).isTrue();
  }
}