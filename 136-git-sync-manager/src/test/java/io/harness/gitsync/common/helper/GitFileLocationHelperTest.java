/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GitFileLocationHelperTest extends CategoryTest {
  final String rootPath = "rootPath";
  final EntityType entityType = EntityType.CONNECTORS;
  final String entityIdentifier = "id";
  final String filePath = rootPath + PATH_DELIMITER + entityType.getYamlName() + PATH_DELIMITER + entityIdentifier
      + EXTENSION_SEPARATOR + YAML_EXTENSION;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void getEntityPath() {
    final String entityPath = GitFileLocationHelper.getEntityPath(rootPath, entityType.getYamlName(), entityIdentifier);
    assertThat(entityPath).isEqualTo(filePath);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void getRootPathSafely() {
    final String rootPathSafely = GitFileLocationHelper.getRootPathSafely(filePath);
    assertThat(rootPathSafely).isEqualTo(rootPath);
    final String rootPathSafely1 = GitFileLocationHelper.getRootPathSafely(entityType.name());
    assertThat(rootPathSafely1).isEqualTo("");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void getEntityType() {
    final EntityType entityTypeRet = GitFileLocationHelper.getEntityType(filePath);
    assertThat(entityTypeRet).isEqualTo(entityType);
  }
}
