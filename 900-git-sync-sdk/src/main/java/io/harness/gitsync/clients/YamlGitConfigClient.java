/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.clients;

import io.harness.gitsync.common.dtos.GitSyncConfigDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YamlGitConfigClient {
  @GET("git-sync")
  Call<List<GitSyncConfigDTO>> getConfigs(@Query("accountIdentifier") String accountIdentifier,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier);
}
