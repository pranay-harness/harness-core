/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.config;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactoryConfigTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDefaultUserNameEmpty() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");

    // Builder default
    ArtifactoryConfig artifactoryConfig1 = ArtifactoryConfig.builder().artifactoryUrl("some registry url").build();
    assertThat(artifactoryConfig1.getUsername()).isNotNull().isEqualTo("");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldArtifactoryHasCredentials() {
    ArtifactoryConfig artifactoryConfig = ArtifactoryConfig.builder()
                                              .artifactoryUrl("some registry url")
                                              .username("some username")
                                              .password("some password".toCharArray())
                                              .build();
    assertThat(artifactoryConfig.hasCredentials()).isTrue();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldArtifactoryHasNoCredentials() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");
    assertThat(artifactoryConfig.hasCredentials()).isFalse();
  }
}
