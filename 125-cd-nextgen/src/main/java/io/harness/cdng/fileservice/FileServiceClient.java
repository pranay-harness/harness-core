/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.fileservice;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.FileBucket;
import io.harness.rest.RestResponse;

import javax.validation.constraints.NotNull;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(CDP)
public interface FileServiceClient {
  String FILE_SERVICE_API = "ng/file-service";

  @GET(FILE_SERVICE_API + "/latestFileId")
  Call<RestResponse<String>> getLatestFileId(
      @Query(value = "entityId") @NotNull String entityId, @Query(value = "fileBucket") @NotNull FileBucket fileBucket);

  @POST(FILE_SERVICE_API + "/parentEntityIdAndVersion")
  Call<RestResponse<Boolean>> updateParentEntityIdAndVersion(@Query(value = "entityId") @NotNull String entityId,
      @Query(value = "stateFileId") @NotNull String stateFileId,
      @Query(value = "fileBucket") @NotNull FileBucket fileBucket);
}
