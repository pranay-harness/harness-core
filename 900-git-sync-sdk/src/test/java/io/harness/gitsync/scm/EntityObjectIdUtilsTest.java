/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.scm;

import static io.harness.rule.OwnerRule.ABHINAV;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DX)
public class EntityObjectIdUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testObjectId() throws IOException {
    String yaml = IOUtils.resourceToString("testYaml.yaml", UTF_8, this.getClass().getClassLoader());
    final String objectIdOfYaml = EntityObjectIdUtils.getObjectIdOfYaml(yaml);
    assertThat(objectIdOfYaml).isEqualTo("afd399ea525e7c8fdf0748415e06961fadf192c7");
  }
}
