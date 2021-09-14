/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.elk;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by rsingh on 8/01/17.
 */
@OwnedBy(HarnessTeam.CV)
public interface ElkRestClient {
  @POST("{index}/_search")
  Call<Object> search(@Path("index") String index, @Body Object elkLogFetchRequest, @Query("size") int size);

  @GET("_template") Call<Map<String, Map<String, Object>>> template();

  @POST("{index}/_search?size=1")
  Call<Object> getLogSample(@Path("index") String index, @Body Object elkLogFetchRequest);
}
