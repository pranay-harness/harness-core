/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.sumo;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SumoLogicSetupTestNodedataTest extends CategoryTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField_whenSourceHost() {
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField("_sourceHost").build();
    assertThat(sourceHost.getHostNameField()).isEqualTo("_sourcehost");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField_whenSourceName() {
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField("_sourceName").build();
    assertThat(sourceHost.getHostNameField()).isEqualTo("_sourcename");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField() {
    String hostNameField = generateUuid();
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField(hostNameField).build();
    assertThat(sourceHost.getHostNameField()).isEqualTo(hostNameField);
  }
}
