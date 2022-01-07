/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.dtos.YamlGitConfigGitFileChangeMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class YamlGitConfigHelper {
  public static List<YamlGitConfigGitFileChangeMap> batchGitFileChangeByRootFolder(
      List<GitFileChange> gitFileChanges, List<YamlGitConfigDTO> yamlGitConfigDTOs) {
    Map<String, YamlGitConfigDTO> rootFolderYamlGitConfig = new HashMap<>();
    yamlGitConfigDTOs.forEach(yamlGitConfigDTO
        -> yamlGitConfigDTO.getRootFolders().forEach(
            rootFolder -> rootFolderYamlGitConfig.put(rootFolder.getRootFolder(), yamlGitConfigDTO)));
    final List<String> rootFolders = yamlGitConfigDTOs.stream()
                                         .flatMap(yamlGitConfigDTO -> yamlGitConfigDTO.getRootFolders().stream())
                                         .map(YamlGitConfigDTO.RootFolder::getRootFolder)
                                         .collect(Collectors.toList());
    final Map<String, List<GitFileChange>> gitFileChangeRootPathMap = gitFileChanges.stream().collect(
        Collectors.groupingBy(gitFileChange -> GitFileLocationHelper.getRootPathSafely(gitFileChange.getFilePath())));

    List<String> unkownPaths = new ArrayList<>();
    final List<YamlGitConfigGitFileChangeMap> yamlGitConfigGitFileChangeMaps =
        gitFileChangeRootPathMap.keySet()
            .stream()
            .map(rootPath -> {
              if (rootFolders.contains(rootPath)) {
                return YamlGitConfigGitFileChangeMap.builder()
                    .gitFileChanges(gitFileChangeRootPathMap.get(rootPath))
                    .yamlGitConfigDTO(rootFolderYamlGitConfig.get(rootPath))
                    .build();
              } else {
                unkownPaths.add(rootPath);
                return null;
              }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    log.error("Couldn't identify filePaths {} ", unkownPaths);
    return yamlGitConfigGitFileChangeMaps;
  }

  // Parse root folders from all yaml git configs
  public Set<String> getRootFolderList(List<YamlGitConfigDTO> yamlGitConfigDTOList) {
    Set<String> rootFolderList = new HashSet<>();
    yamlGitConfigDTOList.forEach(yamlGitConfigDTO
        -> yamlGitConfigDTO.getRootFolders().forEach(rootFolder -> rootFolderList.add(rootFolder.getRootFolder())));
    return rootFolderList;
  }
}
