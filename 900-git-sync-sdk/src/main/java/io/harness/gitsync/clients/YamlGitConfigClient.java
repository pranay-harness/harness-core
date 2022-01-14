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
