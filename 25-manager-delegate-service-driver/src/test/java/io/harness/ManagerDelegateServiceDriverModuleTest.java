package io.harness;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.category.element.UnitTests;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.rule.Owner;
import io.harness.version.VersionInfoManager;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManagerDelegateServiceDriverModuleTest extends CategoryTest {
  private ManagerDelegateServiceDriverModule managerDelegateServiceDriverModule;
  private final String DEV_HARNESS_AUTHORITY = "dev.harness.io";
  private static final String SERVICE_ID = "ng-manager";

  public static class ManagerDelegateServiceDriverTestModule extends ManagerDelegateServiceDriverModule {
    public ManagerDelegateServiceDriverTestModule(GrpcClientConfig grpcClientConfig, String serviceSecret) {
      super(grpcClientConfig, serviceSecret, SERVICE_ID);
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
    managerDelegateServiceDriverModule = new ManagerDelegateServiceDriverTestModule(
        GrpcClientConfig.builder().authority(DEV_HARNESS_AUTHORITY).target("122.171.30.255:7143").build(),
        "service_secret");

    List<Module> modules = new ArrayList<>();
    modules.add(managerDelegateServiceDriverModule);
    Injector injector = Guice.createInjector(modules);

    NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub ngDelegateTaskServiceBlockingStub =
        injector.getInstance(NgDelegateTaskServiceGrpc.NgDelegateTaskServiceBlockingStub.class);
    assertThat(ngDelegateTaskServiceBlockingStub).isNotNull();
    assertThat(ngDelegateTaskServiceBlockingStub.getChannel().authority()).isEqualTo(DEV_HARNESS_AUTHORITY);
    assertThat(ngDelegateTaskServiceBlockingStub.getCallOptions().getCredentials()).isNotNull();
    assertThat(ngDelegateTaskServiceBlockingStub.getCallOptions().getCredentials())
        .isInstanceOf(ServiceAuthCallCredentials.class);
    ServiceAuthCallCredentials callCredentials =
        (ServiceAuthCallCredentials) ngDelegateTaskServiceBlockingStub.getCallOptions().getCredentials();
    assertThat(callCredentials.getServiceId()).isEqualTo(SERVICE_ID);
  }
}
