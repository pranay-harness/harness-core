package io.harness.grpc.pingpong;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.Channel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import io.harness.CategoryTest;
import io.harness.MockableTestMixin;
import io.harness.category.element.UnitTests;
import io.harness.delegate.service.DelegateServiceImpl;
import io.harness.event.PingPongServiceGrpc;
import io.harness.event.PingPongServiceGrpc.PingPongServiceBlockingStub;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.rule.Owner;
import io.harness.security.TokenAuthenticator;
import io.harness.security.TokenGenerator;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class PingPongTest extends CategoryTest implements MockableTestMixin {
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private PingPongClient pingPongClient;
  private Server server;
  private Logger mockClientLogger;
  private Logger mockServerLogger;

  @Before
  public void setUp() throws Exception {
    mockClientLogger = mock(Logger.class);
    mockServerLogger = mock(Logger.class);
    setStaticFieldValue(PingPongClient.class, "logger", mockClientLogger);
    setStaticFieldValue(PingPongService.class, "logger", mockServerLogger);
    setStaticFieldValue(DelegateServiceImpl.class, "delegateId", "DELEGATE_ID");
    String serverName = InProcessServerBuilder.generateName();
    Channel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).build());
    final TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    when(tokenGenerator.getToken(anyString(), anyString())).thenReturn("TOKEN");
    PingPongServiceBlockingStub pingPongServiceBlockingStub =
        PingPongServiceGrpc.newBlockingStub(channel).withCallCredentials(
            new DelegateAuthCallCredentials(tokenGenerator, "ACCOUNT_ID", false));
    pingPongClient = new PingPongClient(pingPongServiceBlockingStub, "VERSION");
    server = InProcessServerBuilder.forName(serverName)
                 .directExecutor()
                 .addService(new PingPongService())
                 .intercept(new DelegateAuthServerInterceptor(mock(TokenAuthenticator.class)))
                 .build()
                 .start();
    grpcCleanup.register(server);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldLogPingSuccessOnClient() throws Exception {
    pingPongClient.runOneIteration();
    val captor = ArgumentCaptor.forClass(String.class);
    verify(mockClientLogger).info(captor.capture(), any(Instant.class));
    assertThat(captor.getValue()).matches("Ping at .* successful");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldLogPingSuccessOnServer() throws Exception {
    pingPongClient.runOneIteration();
    val captor = ArgumentCaptor.forClass(String.class);
    verify(mockServerLogger).info(captor.capture(), any(), any(), any());
    assertThat(captor.getValue()).matches("Ping at .* received .*");
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldLogPingFailure() throws Exception {
    server.shutdownNow().awaitTermination();
    pingPongClient.runOneIteration();
    val captor = ArgumentCaptor.forClass(String.class);
    verify(mockClientLogger).error(captor.capture(), any(Throwable.class));
    assertThat(captor.getValue()).isEqualTo("Ping failed");
  }
}