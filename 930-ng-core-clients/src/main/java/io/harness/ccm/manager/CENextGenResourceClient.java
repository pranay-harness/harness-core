/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.manager;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.GET;

@OwnedBy(CE)
public interface CENextGenResourceClient {
  String BASE_API = "ccm/api";

  @GET(BASE_API + "/") Call<ResponseDTO<Boolean>> test();
}
