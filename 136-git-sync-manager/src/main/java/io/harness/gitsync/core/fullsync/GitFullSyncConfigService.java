/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.fullsync;

import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigDTO;
import io.harness.gitsync.fullsync.dtos.GitFullSyncConfigRequestDTO;

import java.util.Optional;

public interface GitFullSyncConfigService {
  GitFullSyncConfigDTO createConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto);

  Optional<GitFullSyncConfigDTO> get(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  GitFullSyncConfigDTO updateConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, GitFullSyncConfigRequestDTO dto);

  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
