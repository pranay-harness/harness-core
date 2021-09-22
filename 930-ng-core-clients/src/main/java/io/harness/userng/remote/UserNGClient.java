package io.harness.userng.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface UserNGClient {
  String ALL_PROJECTS_ACCESSIBLE_TO_USER_API = "user/all-projects";

  @GET(ALL_PROJECTS_ACCESSIBLE_TO_USER_API)
  Call<ResponseDTO<List<ProjectDTO>>> getUserAllProjectsInfo(
      @Query(value = "accountId") String accountId, @Query(value = "userId") String userId);
}
