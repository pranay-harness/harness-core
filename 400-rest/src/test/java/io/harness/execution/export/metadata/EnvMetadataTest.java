/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.EnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.EnvSummary;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromEmbeddedUser() {
    assertThat(EnvMetadata.fromFirstEnvSummary(null)).isNull();
    assertThat(EnvMetadata.fromFirstEnvSummary(Collections.singletonList(null))).isNull();
    assertThat(EnvMetadata.fromFirstEnvSummary(asList(EnvSummary.builder().build(),
                   EnvSummary.builder().name("e").environmentType(EnvironmentType.NON_PROD).build())))
        .isNull();
    EnvMetadata envMetadata = EnvMetadata.fromFirstEnvSummary(
        asList(EnvSummary.builder().name("e1").environmentType(EnvironmentType.NON_PROD).build(),
            EnvSummary.builder().name("e2").environmentType(EnvironmentType.PROD).build()));
    assertThat(envMetadata).isNotNull();
    assertThat(envMetadata.getName()).isEqualTo("e1");
    assertThat(envMetadata.getEnvironmentType()).isEqualTo(EnvironmentType.NON_PROD);
  }
}
