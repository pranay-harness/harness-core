package io.harness.accesscontrol;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentAggregateResponseDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentCreateRequestDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentFilterDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
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
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlAdminClient {
  String ROLE_ASSIGNMENTS_API = "roleassignments";

  @GET(ROLE_ASSIGNMENTS_API)
  Call<ResponseDTO<PageResponse<RoleAssignmentResponseDTO>>> getRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size);

  @POST(ROLE_ASSIGNMENTS_API + "/filter")
  Call<ResponseDTO<PageResponse<RoleAssignmentResponseDTO>>> getFilteredRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @Body RoleAssignmentFilterDTO roleAssignmentFilterDTO);

  @POST(ROLE_ASSIGNMENTS_API + "/aggregate")
  Call<ResponseDTO<RoleAssignmentAggregateResponseDTO>> getAggregatedFilteredRoleAssignments(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body RoleAssignmentFilterDTO roleAssignmentFilterDTO);

  @POST(ROLE_ASSIGNMENTS_API)
  Call<ResponseDTO<RoleAssignmentResponseDTO>> createRoleAssignment(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Body RoleAssignmentDTO roleAssignmentDTO);

  @DELETE(ROLE_ASSIGNMENTS_API + "/{identifier}")
  Call<ResponseDTO<RoleAssignmentResponseDTO>> deleteRoleAssignment(@Path("identifier") String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(ROLE_ASSIGNMENTS_API + "/multi")
  Call<ResponseDTO<List<RoleAssignmentResponseDTO>>> createMulti(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO);
}
