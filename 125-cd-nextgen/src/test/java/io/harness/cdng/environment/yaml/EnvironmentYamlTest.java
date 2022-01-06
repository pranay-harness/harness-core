/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.environment.yaml;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.rule.Owner;

import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvironmentYamlTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    EnvironmentYaml environmentYaml = EnvironmentYaml.builder()
                                          .identifier("identifer")
                                          .name("identifer")
                                          .tags(singletonMap("key1", "value1"))
                                          .type(EnvironmentType.PreProduction)
                                          .build();

    NGTag newTag = NGTag.builder().key("new-key1").value("new-value1").build();
    EnvironmentYaml overrideEnvYaml = EnvironmentYaml.builder()
                                          .identifier("new-identifer")
                                          .name("new-name")
                                          .tags(Collections.singletonMap("new-key1", "new-value1"))
                                          .type(EnvironmentType.Production)
                                          .build();

    EnvironmentYaml overriddenEnvironment = environmentYaml.applyOverrides(overrideEnvYaml);

    assertThat(overriddenEnvironment.getName()).isEqualTo("new-name");
    assertThat(overriddenEnvironment.getIdentifier()).isEqualTo("new-identifer");
    assertThat(overriddenEnvironment.getType()).isEqualTo(EnvironmentType.Production);
    assertThat(overriddenEnvironment.getTags().containsKey("new-key1")).isTrue();
    assertThat(overriddenEnvironment.getTags().get("new-key1")).isEqualTo("new-value1");
  }
}
