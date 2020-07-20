package io.harness.organizationmanagerclient.remote;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.CreateOrganizationDTO;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UpdateOrganizationDTO;
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
  String ACCOUNT_IDENTIFIER = "accountIdentifier";
  String ORG_IDENTIFIER = "orgIdentifier";

  String ORGANIZATIONS_API = "/accounts/{accountIdentifier}/organizations";

  @POST(ORGANIZATIONS_API)
  Call<ResponseDTO<OrganizationDTO>> createOrganization(
      @Path(value = ACCOUNT_IDENTIFIER) String accountIdentifier, @Body CreateOrganizationDTO request);

  @GET(ORGANIZATIONS_API + "/{organizationIdentifier}")
  Call<ResponseDTO<Optional<OrganizationDTO>>> getOrganization(
      @Path(value = ACCOUNT_IDENTIFIER) String accountIdentifier,
      @Path(value = ORG_IDENTIFIER) String organizationIdentifier);

  @GET(ORGANIZATIONS_API)
  Call<ResponseDTO<NGPageResponse<OrganizationDTO>>> listOrganization(
      @Path(value = ACCOUNT_IDENTIFIER) String accountIdentifier, @Query(value = "page") int page,
      @Query(value = "size") int size, @Query(value = "sort") List<String> sort);

  @PUT(ORGANIZATIONS_API + "/{organizationIdentifier}")
  Call<ResponseDTO<Optional<OrganizationDTO>>> updateOrganization(
      @Path(value = ACCOUNT_IDENTIFIER) String accountIdentifier, @Path(value = ORG_IDENTIFIER) String orgIdentifier,
      @Body UpdateOrganizationDTO updateOrganizationDTO);

  @DELETE(ORGANIZATIONS_API + "/{organizationIdentifier}")
  Call<ResponseDTO<Boolean>> deleteOrganization(@Path(value = ACCOUNT_IDENTIFIER) String accountIdentifier,
      @Path(value = ORG_IDENTIFIER) String organizationIdentifier);
}
