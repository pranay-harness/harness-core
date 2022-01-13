/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.controller;

import static io.harness.beans.PrimaryVersion.Builder.aConfiguration;
import static io.harness.beans.PrimaryVersion.GLOBAL_CONFIG_ID;
import static io.harness.beans.PrimaryVersion.MATCH_ALL_VERSION;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.PrimaryVersionManagerTestBase;
import io.harness.beans.PrimaryVersion;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.version.VersionInfo;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrimaryVersionControllerTest extends PrimaryVersionManagerTestBase {
  @Inject PrimaryVersionController primaryVersionController;
  @Inject HPersistence persistence;
  @Mock VersionInfoManager versionInfoManager;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(primaryVersionController, "persistence", persistence, true);
    FieldUtils.writeField(primaryVersionController, "versionInfoManager", versionInfoManager, true);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRun_NoPrimaryVersion() {
    mockVersionInfo("1.1.0");
    primaryVersionController.run();
    assertThat(primaryVersionController.isPrimary()).isTrue();
    assertThat(primaryVersionController.getPrimaryVersion()).isEqualTo(MATCH_ALL_VERSION);

    PrimaryVersion primaryVersion = persistence.createQuery(PrimaryVersion.class).get();
    assertThat(primaryVersion).isNotNull();
    assertThat(primaryVersion.getUuid()).isEqualTo(GLOBAL_CONFIG_ID);
    assertThat(primaryVersion.getPrimaryVersion()).isEqualTo(MATCH_ALL_VERSION);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRun_DefaultVersion() {
    persistence.save(aConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build());
    mockVersionInfo("1.1.0");
    primaryVersionController.run();
    assertThat(primaryVersionController.isPrimary()).isTrue();
    assertThat(primaryVersionController.getPrimaryVersion()).isEqualTo(MATCH_ALL_VERSION);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRun_VersionMatch() {
    String version = "1.1.0";
    persistence.save(aConfiguration().withPrimaryVersion(version).build());
    mockVersionInfo(version);
    primaryVersionController.run();
    assertThat(primaryVersionController.isPrimary()).isTrue();
    assertThat(primaryVersionController.getPrimaryVersion()).isEqualTo(version);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRun_VersionMisMatch() {
    String version = "1.1.0";
    persistence.save(aConfiguration().withPrimaryVersion(version).build());
    mockVersionInfo("1.2.5");
    primaryVersionController.run();
    assertThat(primaryVersionController.isPrimary()).isFalse();
    assertThat(primaryVersionController.getPrimaryVersion()).isEqualTo(version);
  }

  private void mockVersionInfo(String version) {
    VersionInfo mockedVersion = VersionInfo.builder().version(version).build();
    doReturn(mockedVersion).when(versionInfoManager).getVersionInfo();
  }
}
