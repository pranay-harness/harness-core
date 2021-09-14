/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.instana;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.instana.InstanaAnalyzeMetricRequest;
import software.wings.service.impl.instana.InstanaAnalyzeMetrics;
import software.wings.service.impl.instana.InstanaInfraMetricRequest;
import software.wings.service.impl.instana.InstanaInfraMetrics;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
@OwnedBy(HarnessTeam.CV)
@TargetModule(_960_API_SERVICES)
public interface InstanaRestClient {
  @POST("api/infrastructure-monitoring/metrics/")
  Call<InstanaInfraMetrics> getInfrastructureMetrics(
      @Header("Authorization") String authorization, @Body InstanaInfraMetricRequest request);
  @POST("api/application-monitoring/analyze/trace-groups")
  Call<InstanaAnalyzeMetrics> getGroupedTraceMetrics(
      @Header("Authorization") String authorization, @Body InstanaAnalyzeMetricRequest request);
}
