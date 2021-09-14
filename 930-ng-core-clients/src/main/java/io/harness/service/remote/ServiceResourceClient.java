/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.remote;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponse;

import java.util.List;
import javax.ws.rs.DefaultValue;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PIPELINE)
public interface ServiceResourceClient {
  String SERVICE_API = "servicesV2";

  @GET(SERVICE_API)
  Call<ResponseDTO<PageResponse<ServiceResponse>>> listServicesForProject(@Query("page") @DefaultValue("0") int page,
      @Query("size") @DefaultValue("100") int size, @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("serviceIdentifiers") List<String> serviceIdentifiers, @Query("sort") List<String> sort);
}
