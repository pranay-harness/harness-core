package io.harness.ngaggregate.remote;

import io.harness.ng.core.dto.ActiveProjectsCountDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NGAggregateClient {
  String ALL_PROJECTS_ACCESSIBLE_TO_USER_API = "aggregate/all-projects";
  String COUNT_OF_ACCESSIBLE_PROJECTS_API = "aggregate/projects-count";

  @GET(ALL_PROJECTS_ACCESSIBLE_TO_USER_API)
  Call<ResponseDTO<List<ProjectDTO>>> getUserAllProjectsInfo(@Query(value = "accountIdentifier") String accountId);

  @GET(COUNT_OF_ACCESSIBLE_PROJECTS_API)
  Call<ResponseDTO<ActiveProjectsCountDTO>> getAccessibleProjectsCount(
      @Query(value = "accountIdentifier") String accountId, @Query(value = "startTime") long startInterval,
      @Query(value = "endTime") long endInterval);
}
