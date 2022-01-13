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
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.dtos.YamlGitConfigGitFileChangeMap;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class YamlGitConfigHelperTest extends CategoryTest {
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void batchGitFileChangeByRootFolder() {
    final String rootPath = "rootPath";
    final String rootPath1 = "rootPath1";
    final EntityType connector = EntityType.CONNECTORS;
    final EntityType pipelines = EntityType.PIPELINES;
    final String entityIdentifier = "id";
    final String filePathConnector = rootPath + PATH_DELIMITER + connector.getYamlName() + PATH_DELIMITER
        + entityIdentifier + EXTENSION_SEPARATOR + YAML_EXTENSION;
    final String filePathPipeline = rootPath1 + PATH_DELIMITER + pipelines.getYamlName() + PATH_DELIMITER
        + entityIdentifier + EXTENSION_SEPARATOR + YAML_EXTENSION;
    final List<GitFileChange> gitFileChanges = Arrays.asList(GitFileChange.builder().filePath(filePathPipeline).build(),
        GitFileChange.builder().filePath(filePathConnector).build());
    final YamlGitConfigDTO yamlGitConfigDTO =
        YamlGitConfigDTO.builder()
            .rootFolders(Collections.singletonList(YamlGitConfigDTO.RootFolder.builder().rootFolder(rootPath).build()))
            .build();
    final YamlGitConfigDTO yamlGitConfigDTO1 =
        YamlGitConfigDTO.builder()
            .rootFolders(Collections.singletonList(YamlGitConfigDTO.RootFolder.builder().rootFolder(rootPath1).build()))
            .build();

    final List<YamlGitConfigGitFileChangeMap> yamlGitConfigGitFileChangeMaps =
        YamlGitConfigHelper.batchGitFileChangeByRootFolder(
            gitFileChanges, Arrays.asList(yamlGitConfigDTO, yamlGitConfigDTO1));
    assertThat(yamlGitConfigGitFileChangeMaps.size()).isEqualTo(2);
    assertThat(yamlGitConfigGitFileChangeMaps.get(0).getGitFileChanges()).isNotNull();
    assertThat(yamlGitConfigGitFileChangeMaps.get(1).getGitFileChanges()).isNotNull();
    assertThat(yamlGitConfigGitFileChangeMaps.get(0).getYamlGitConfigDTO()).isNotNull();
    assertThat(yamlGitConfigGitFileChangeMaps.get(1).getYamlGitConfigDTO()).isNotNull();
  }
}
