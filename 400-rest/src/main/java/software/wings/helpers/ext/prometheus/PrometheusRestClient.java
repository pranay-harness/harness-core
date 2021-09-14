/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
