package io.harness.projectmanagerclient.remote;

import static io.harness.NGConstants.ACCOUNT_KEY;
import static io.harness.NGConstants.IDENTIFIER_KEY;
import static io.harness.NGConstants.MODULE_TYPE_KEY;
import static io.harness.NGConstants.ORG_KEY;
import static io.harness.NGConstants.PAGE_KEY;
import static io.harness.NGConstants.SEARCH_TERM_KEY;
import static io.harness.NGConstants.SIZE_KEY;
import static io.harness.NGConstants.SORT_KEY;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;

import io.harness.ModuleType;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectManagerClient {
  String PROJECTS_API = "projects";

  @POST(PROJECTS_API)
  Call<ResponseDTO<ProjectDTO>> createProject(@Query(value = ACCOUNT_KEY) String accountIdentifier,
      @Query(value = ORG_KEY) String orgIdentifier, @Body ProjectDTO projectDTO);

  @GET(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> getProject(@Path(value = IDENTIFIER_KEY) String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(value = ORG_KEY) String orgIdentifier);

  @GET(PROJECTS_API)
  Call<ResponseDTO<PageResponse<ProjectDTO>>> listProject(@Query(value = ACCOUNT_KEY) String accountIdentifier,
      @Query(value = ORG_KEY) String orgIdentifier, @Query(value = "hasModule") boolean hasModule,
      @Query(value = MODULE_TYPE_KEY) ModuleType moduleType, @Query(value = SEARCH_TERM_KEY) String searchTerm,
      @Query(value = PAGE_KEY) int page, @Query(value = SIZE_KEY) int size, @Query(value = SORT_KEY) List<String> sort);

  @PUT(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Optional<ProjectDTO>>> updateProject(@Path(value = IDENTIFIER_KEY) String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Query(value = ORG_KEY) String orgIdentifier,
      @Body ProjectDTO projectDTO);

  @DELETE(PROJECTS_API + "/{identifier}")
  Call<ResponseDTO<Boolean>> deleteProject(@Header(IF_MATCH) Long ifMatch,
      @Path(value = IDENTIFIER_KEY) String identifier, @Path(value = ACCOUNT_KEY) String accountIdentifier,
      @Path(value = ORG_KEY) String orgIdentifier);
}
