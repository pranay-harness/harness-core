/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
    assertThat(objectIdOfYaml).isEqualTo("e1ba3a974b9300b862c53023a3aeee5735b7d914");
  }
}
