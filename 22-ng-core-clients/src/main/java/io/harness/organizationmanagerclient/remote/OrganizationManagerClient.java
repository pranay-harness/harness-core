package io.harness.organizationmanagerclient.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.PAGE_KEY;
import static io.harness.ng.NGConstants.SEARCH_TERM_KEY;
import static io.harness.ng.NGConstants.SIZE_KEY;
import static io.harness.ng.NGConstants.SORT_KEY;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ResponseDTO;
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

public interface OrganizationManagerClient {
  String ORGANIZATIONS_API = "/organizations";

  @POST(ORGANIZATIONS_API)
  Call<ResponseDTO<OrganizationDTO>> createOrganization(
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Body OrganizationDTO organizationDTO);

  @GET(ORGANIZATIONS_API + "/{identifier}")
  Call<ResponseDTO<Optional<OrganizationDTO>>> getOrganization(
      @Path(value = IDENTIFIER_KEY) String identifier, @Query(value = ACCOUNT_KEY) String accountIdentifier);

  @GET(ORGANIZATIONS_API)
  Call<ResponseDTO<NGPageResponse<OrganizationDTO>>> listOrganization(
      @Path(value = ACCOUNT_KEY) String accountIdentifier, @Query(SEARCH_TERM_KEY) String searchTerm,
      @Query(value = PAGE_KEY) int page, @Query(value = SIZE_KEY) int size, @Query(value = SORT_KEY) List<String> sort);

  @PUT(ORGANIZATIONS_API + "/{identifier}")
  Call<ResponseDTO<Optional<OrganizationDTO>>> updateOrganization(@Path(value = IDENTIFIER_KEY) String identifier,
      @Query(value = ACCOUNT_KEY) String accountIdentifier, @Body OrganizationDTO organizationDTO);

  @DELETE(ORGANIZATIONS_API + "/{identifier}")
  Call<ResponseDTO<Boolean>> deleteOrganization(
      @Path(value = IDENTIFIER_KEY) String identifier, @Query(value = ACCOUNT_KEY) String accountIdentifier);
}
