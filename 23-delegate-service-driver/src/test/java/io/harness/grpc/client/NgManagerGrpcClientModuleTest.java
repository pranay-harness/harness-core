package io.harness.grpc.client;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.grpc.ng.NgManagerGrpcClientModule;
import io.harness.rule.Owner;
import io.harness.version.VersionInfoManager;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NgManagerGrpcClientModuleTest extends CategoryTest {
  private NgManagerGrpcClientModule ngManagerGrpcClientModule;
  private final String DEV_HARNESS_AUTHORITY = "dev.harness.io";

  public static class NgManagerGrpcClientTestModule extends NgManagerGrpcClientModule {
    public NgManagerGrpcClientTestModule(GrpcClientConfig grpcClientConfig, String serviceSecret) {
      super(grpcClientConfig, serviceSecret);
    }

    @Override
    protected void configure() {
      Map<String, Object> data = new HashMap<>();
      data.put("buildNo", " 1.0.55800");
      data.put("version", " 1.0.55800");

      Yaml yaml = new Yaml();
      String yamlAsString = yaml.dump(data);

      VersionInfoManager versionInfoManager = new VersionInfoManager(yamlAsString);
      bind(VersionInfoManager.class).toProvider(() -> versionInfoManager);

      super.configure();
    }
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForIpAddressAuthority() {
    ngManagerGrpcClientModule = new NgManagerGrpcClientTestModule(
        GrpcClientConfig.builder().authority(DEV_HARNESS_AUTHORITY).target("122.171.30.255:7143").build(),
        "service_secret");

    List<Module> modules = new ArrayList<>();
    modules.add(ngManagerGrpcClientModule);
    Injector injector = Guice.createInjector(modules);

    NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub =
        injector.getInstance(NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub.class);
    assertThat(ngDelegateTaskServiceBlockingStub).isNotNull();
    assertThat(ngDelegateTaskServiceBlockingStub.getChannel().authority()).isEqualTo(DEV_HARNESS_AUTHORITY);
  }
}
