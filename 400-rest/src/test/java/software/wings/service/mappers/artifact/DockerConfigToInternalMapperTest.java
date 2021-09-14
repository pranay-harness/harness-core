/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.mappers.artifact;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.DockerConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DockerConfigToInternalMapperTest extends CategoryTest {
  DockerConfig dockerConfig;

  @Before
  public void setUp() throws Exception {
    dockerConfig = DockerConfig.builder()
                       .dockerRegistryUrl("REGISTRY_URL")
                       .username("USERNAME")
                       .password("PASSWORD".toCharArray())
                       .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToDockerInternalConfig() {
    DockerInternalConfig dockerInternalConfig = DockerConfigToInternalMapper.toDockerInternalConfig(dockerConfig);
    assertThat(dockerInternalConfig).isNotNull();
    assertThat(dockerInternalConfig.getDockerRegistryUrl()).isEqualTo(dockerConfig.getDockerRegistryUrl());
    assertThat(dockerInternalConfig.getUsername()).isEqualTo(dockerConfig.getUsername());
    assertThat(dockerInternalConfig.getPassword()).isEqualTo(new String(dockerConfig.getPassword()));
  }
}
