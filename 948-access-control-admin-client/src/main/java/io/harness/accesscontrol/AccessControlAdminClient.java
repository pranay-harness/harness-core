package io.harness.accesscontrol;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlAdminClient {
  String ROLE_ASSIGNMENTS_API = "roleassignments";
  String ROLE_API = "roles";
  String ACCESS_CONTROL_PREFERENCE_API = "accessControlPreferences";

  @POST(ROLE_ASSIGNMENTS_API + "/filter")
  Call<ResponseDTO<PageResponse<RoleAssignmentResponseDTO>>> getFilteredRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @Body RoleAssignmentFilterDTO roleAssignmentFilterDTO);

  @PUT(ACCESS_CONTROL_PREFERENCE_API)
  Call<ResponseDTO<Boolean>> upsertAccessControlPreference(
      @Query("accountIdentifier") String accountIdentifier, @Query("enabled") boolean enabled);

  @POST(ROLE_ASSIGNMENTS_API + "/aggregate")
  Call<ResponseDTO<RoleAssignmentAggregateResponseDTO>> getAggregatedFilteredRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body RoleAssignmentFilterDTO roleAssignmentFilterDTO);

  @POST(ROLE_ASSIGNMENTS_API + "/multi/internal")
  Call<ResponseDTO<List<RoleAssignmentResponseDTO>>> createMultiRoleAssignment(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Query("managed") Boolean managed,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO);

  @DELETE(ROLE_ASSIGNMENTS_API + "/{identifier}")
  Call<ResponseDTO<RoleAssignmentResponseDTO>> deleteRoleAssignment(@Path("identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @GET(ROLE_API)
  Call<ResponseDTO<PageResponse<RoleResponseDTO>>> getRoles(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size);

  @GET(ROLE_API + "/{identifier}")
  Call<ResponseDTO<PageResponse<RoleResponseDTO>>> getRole(
      @Path(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
