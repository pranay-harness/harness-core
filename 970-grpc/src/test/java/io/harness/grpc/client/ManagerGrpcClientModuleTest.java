/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.client;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.grpc.client.ManagerGrpcClientModule.Config;
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
    managerGrpcClientModule = ManagerGrpcClientModule.getInstance();
    assertThatCode(
        ()
            -> managerGrpcClientModule.managerChannel(
                Config.builder().authority("manager-grpc-104.198.111.162:7143").target("104.198.111.162:7143").build(),
                "Delegate", versionInfoManager))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForInvalidVersion() throws Exception {
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("${build.fullVersion}").build());
    managerGrpcClientModule = ManagerGrpcClientModule.getInstance();
    assertThatCode(
        ()
            -> managerGrpcClientModule.managerChannel(
                Config.builder().authority("manager-grpc-app.harness.io:443").target("app.harness.io:443").build(),
                "Manager", versionInfoManager))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldConstructManagerChannelForValidVersion() throws Exception {
    when(versionInfoManager.getVersionInfo()).thenReturn(VersionInfo.builder().version("1.0.42104").build());
    managerGrpcClientModule = ManagerGrpcClientModule.getInstance();
    assertThatCode(
        ()
            -> managerGrpcClientModule.managerChannel(
                Config.builder().authority("manager-grpc-app.harness.io:443").target("app.harness.io:443").build(),
                "Manager", versionInfoManager))
        .doesNotThrowAnyException();
  }
}
