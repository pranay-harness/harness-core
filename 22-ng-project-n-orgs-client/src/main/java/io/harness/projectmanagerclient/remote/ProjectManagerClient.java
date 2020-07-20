package io.harness.projectmanagerclient.remote;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.CreateProjectDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UpdateProjectDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectManagerClient {
  String PROJECT_IDENTIFIER = "projectIdentifier";
  String ORG_IDENTIFIER = "orgIdentifier";

  String PROJECTS_API = "/organizations/{orgIdentifier}/projects";

  @POST(PROJECTS_API)
  Call<ResponseDTO<ProjectDTO>> createProject(
      @Path(value = ORG_IDENTIFIER) String orgIdentifier, @Body CreateProjectDTO createProjectDTO);

  @GET(PROJECTS_API + "/{projectIdentifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> getProject(
      @Path(value = ORG_IDENTIFIER) String orgIdentifier, @Path(value = PROJECT_IDENTIFIER) String projectIdentifier);

  @GET(PROJECTS_API)
  Call<ResponseDTO<NGPageResponse<ProjectDTO>>> listProjectsForOrganization(
      @Path(value = ORG_IDENTIFIER) String orgIdentifier, @Query(value = "page") int page,
      @Query(value = "size") int size, @Query("sort") List<String> sort);

  @PUT(PROJECTS_API + "/{projectIdentifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> updateProject(@Path(value = ORG_IDENTIFIER) String orgIdentifier,
      @Path(value = PROJECT_IDENTIFIER) String projectIdentifier, @Body UpdateProjectDTO updateProjectDTO);

  @DELETE(PROJECTS_API + "/{projectIdentifier}")
  Call<ResponseDTO<Boolean>> deleteProject(
      @Path(value = ORG_IDENTIFIER) String orgIdentifier, @Path(value = PROJECT_IDENTIFIER) String projectIdentifier);
}
