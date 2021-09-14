/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.usergroups;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface UserGroupClient {
  String USER_GROUP_BASEURI = "user-groups";

  @GET(USER_GROUP_BASEURI + "/{identifier}")
  Call<ResponseDTO<UserGroupDTO>> getUserGroup(@Path("identifier") String identifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(USER_GROUP_BASEURI + "/batch")
  Call<ResponseDTO<List<UserGroupDTO>>> getFilteredUserGroups(@Body UserGroupFilterDTO userGroupFilter);

  @GET(USER_GROUP_BASEURI + "/{identifier}/member/{userIdentifier}")
  Call<ResponseDTO<Boolean>> checkMember(@Path("identifier") String identifier,
      @Path("userIdentifier") String userIdentifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);
}
