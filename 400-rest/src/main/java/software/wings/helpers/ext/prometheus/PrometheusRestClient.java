/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.prometheus;

import software.wings.service.impl.prometheus.PrometheusMetricDataResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

/**
 * Created by rsingh on 1/29/18.
 */
public interface PrometheusRestClient {
  @GET Call<PrometheusMetricDataResponse> fetchMetricData(@Url String url);
}
