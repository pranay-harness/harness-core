/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.logz;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by rsingh on 8/21/17.
 */
public interface LogzRestClient {
  @POST("v1/search?size=10000") Call<Object> search(@Body Object logzFetchRequest);

  @POST("v1/search?size=1") Call<Object> getLogSample(@Body Object logzFetchRequest);
}
