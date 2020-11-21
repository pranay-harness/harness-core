package io.harness.projectmanagerclient.remote;

import static javax.ws.rs.core.HttpHeaders.IF_MATCH;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProjectManagerClient {
  String PROJECTS_API = "projects";

  @POST(PROJECTS_API)
  Call<ResponseDTO<ProjectDTO>> createProject(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Body ProjectDTO projectDTO);

  @GET(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> getProject(
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier);

  @GET(PROJECTS_API)
  Call<ResponseDTO<PageResponse<ProjectDTO>>> listProject(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = "hasModule") boolean hasModule,
      @Query(value = NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @Query(value = NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @Query(value = NGResourceFilterConstants.PAGE_KEY) int page,
      @Query(value = NGResourceFilterConstants.SIZE_KEY) int size,
      @Query(value = NGResourceFilterConstants.SORT_KEY) List<String> sort);

  @PUT(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> updateProject(
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Body ProjectDTO projectDTO);

  @DELETE(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Boolean>> deleteProject(@Header(IF_MATCH) Long ifMatch,
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Path(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Path(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier);
}
