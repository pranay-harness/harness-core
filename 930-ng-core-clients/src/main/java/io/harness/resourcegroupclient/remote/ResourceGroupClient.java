/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroupclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupFilterDTO;
import io.harness.resourcegroup.remote.dto.ResourceGroupRequest;
import io.harness.resourcegroupclient.ResourceGroupResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PL)
public interface ResourceGroupClient {
  String RESOURCE_GROUP_API = "resourcegroup";

  @POST(RESOURCE_GROUP_API)
  Call<ResponseDTO<ResourceGroupResponse>> create(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Body ResourceGroupRequest resourceGroupRequest);

  @POST(RESOURCE_GROUP_API + "/createManaged")
  Call<ResponseDTO<Boolean>> createManagedResourceGroup(
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @GET(RESOURCE_GROUP_API + "/{identifier}")
  Call<ResponseDTO<ResourceGroupResponse>> getResourceGroup(
      @Path(value = NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(value = NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(value = NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(value = NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(RESOURCE_GROUP_API + "/filter")
  Call<ResponseDTO<PageResponse<ResourceGroupResponse>>> getFilteredResourceGroups(
      @Body ResourceGroupFilterDTO resourceGroupFilter, @Query(value = NGResourceFilterConstants.PAGE_KEY) int page,
      @Query(value = NGResourceFilterConstants.SIZE_KEY) int size);
}
