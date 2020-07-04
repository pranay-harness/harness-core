package io.harness.grpc;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import io.grpc.CallCredentials;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc.NgDelegateTaskResponseServiceBlockingStub;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class GrpcClientModuleTest extends CategoryTest {
  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testConfigure() {
    String serviceSecret = "service_secret";
    String authority = "localhost";
    String target = "localhost:9980";

    GrpcClientConfig grpcClientConfig = GrpcClientConfig.builder().target(target).authority(authority).build();
    GrpcClientModule grpcClientModule = new GrpcClientModule(grpcClientConfig, serviceSecret);

    List<Module> modules = new ArrayList<>();
    modules.add(grpcClientModule);
    Injector injector = Guice.createInjector(modules);

    NgDelegateTaskResponseServiceBlockingStub ngDelegateTaskResponseServiceBlockingStub =
        injector.getInstance(NgDelegateTaskResponseServiceBlockingStub.class);
    assertThat(ngDelegateTaskResponseServiceBlockingStub).isNotNull();
    assertThat(ngDelegateTaskResponseServiceBlockingStub.getChannel()).isNotNull();

    CallCredentials callCredentials =
        injector.getInstance(Key.get(CallCredentials.class, Names.named("ng-call-credentials")));
    assertThat(callCredentials).isNotNull();
    assertThat(callCredentials instanceof ServiceAuthCallCredentials).isTrue();
  }
}
