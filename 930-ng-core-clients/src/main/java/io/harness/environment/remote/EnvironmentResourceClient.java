/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.environment.remote;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;

import java.util.List;
import javax.ws.rs.DefaultValue;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PIPELINE)
public interface EnvironmentResourceClient {
  String ENVIRONMENT_API = "environmentsV2";

  @GET(ENVIRONMENT_API)
  Call<ResponseDTO<PageResponse<EnvironmentResponse>>> listEnvironmentsForProject(
      @Query("page") @DefaultValue("0") int page, @Query("size") @DefaultValue("100") int size,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("envIdentifiers") List<String> envIdentifiers, @Query("sort") List<String> sort);
}
