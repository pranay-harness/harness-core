/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlAdminClient {
  String ROLE_ASSIGNMENTS_API = "roleassignments";
  String ROLE_API = "roles";
  String ACL_PREFERENCES_API = "aclPreferences";

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

  @POST(ROLE_ASSIGNMENTS_API + "/multi/internal")
  Call<ResponseDTO<List<RoleAssignmentResponseDTO>>> createMultiRoleAssignment(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Query("managed") Boolean managed,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO);

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

  @PUT(ACL_PREFERENCES_API)
  Call<ResponseDTO<Boolean>> updateAccessControlPreference(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @Query("enabled") boolean enabled);
}
