/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.segment.remote;

import io.harness.telemetry.remote.TelemetryDataDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RemoteSegmentClient {
  String SEGMENT_URI = "harness";

  @POST(SEGMENT_URI) Call<TelemetryDataDTO> reportEvent(@Body TelemetryDataDTO segmentDataDTO);
}
