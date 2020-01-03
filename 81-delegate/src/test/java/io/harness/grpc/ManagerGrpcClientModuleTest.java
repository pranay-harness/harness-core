package io.harness.grpc;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.grpc.ManagerGrpcClientModule.Config;
import io.harness.rule.Owner;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ManagerGrpcClientModuleTest extends CategoryTest {
  private VersionInfoManager versionInfoManager;
  private ManagerGrpcClientModule managerGrpcClientModule;

  @Before
  public void setUp() throws Exception {
    versionInfoManager = mock(VersionInfoManager.class);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForIpAddressAuthority() throws Exception {
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("1.0.42104").build());
    managerGrpcClientModule = new ManagerGrpcClientModule(
        Config.builder().authority("manager-grpc-104.198.111.162:7143").target("104.198.111.162:7143").build());
    assertThatCode(() -> managerGrpcClientModule.managerChannel(versionInfoManager)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForInvalidVersion() throws Exception {
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("${build.fullVersion}").build());
    managerGrpcClientModule = new ManagerGrpcClientModule(
        Config.builder().authority("manager-grpc-app.harness.io:443").target("app.harness.io:443").build());
    assertThatCode(() -> managerGrpcClientModule.managerChannel(versionInfoManager)).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForValidVersion() throws Exception {
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("1.0.42104").build());
    managerGrpcClientModule = new ManagerGrpcClientModule(
        Config.builder().authority("manager-grpc-app.harness.io:443").target("app.harness.io:443").build());
    assertThatCode(() -> managerGrpcClientModule.managerChannel(versionInfoManager)).doesNotThrowAnyException();
  }
}