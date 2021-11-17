package io.harness.ngaggregate.remote;

import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NGAggregateClient {
  String ALL_PROJECTS_ACCESSIBLE_TO_USER_API = "aggregate/all-projects";

  @GET(ALL_PROJECTS_ACCESSIBLE_TO_USER_API)
  Call<ResponseDTO<List<ProjectDTO>>> getUserAllProjectsInfo(@Query(value = "accountIdentifier") String accountId);
}
