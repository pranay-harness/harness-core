package io.harness.userng.remote;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ActiveProjectsCountDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface UserNGClient {
  String ALL_PROJECTS_ACCESSIBLE_TO_USER_API = "user/all-projects";
  String COUNT_OF_ACCESSIBLE_PROJECTS_API = "user/projects-count";

  @GET(ALL_PROJECTS_ACCESSIBLE_TO_USER_API)
  Call<ResponseDTO<List<ProjectDTO>>> getUserAllProjectsInfo(
      @Query(value = "accountId") String accountId, @Query(value = "userId") String userId);

  @GET(COUNT_OF_ACCESSIBLE_PROJECTS_API)
  Call<ResponseDTO<ActiveProjectsCountDTO>> getAccessibleProjectsCount(
      @Query(value = "accountIdentifier") String accountId, @Query(value = "userId") String userId,
      @Query(value = "startTime") long startInterval, @Query(value = "endTime") long endInterval);
}
