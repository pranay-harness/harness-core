package io.harness.accesscontrol;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccessControlAdminClient {
  String ROLE_ASSIGNMENTS_API = "/api/roleassignments";

  @GET(ROLE_ASSIGNMENTS_API)
  Call<ResponseDTO<PageResponse<RoleAssignmentDTO>>> getRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size);

  @POST(ROLE_ASSIGNMENTS_API + "/filter")
  Call<ResponseDTO<PageResponse<RoleAssignmentDTO>>> getFilteredRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @Body RoleAssignmentFilterDTO roleAssignmentFilterDTO);

  @POST(ROLE_ASSIGNMENTS_API)
  Call<ResponseDTO<PageResponse<RoleAssignmentDTO>>> createRoleAssignment(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Body RoleAssignmentDTO roleAssignmentDTO);

  @DELETE(ROLE_ASSIGNMENTS_API + "/{identifier}")
  Call<ResponseDTO<PageResponse<RoleAssignmentDTO>>> deleteRoleAssignment(@Path("identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
