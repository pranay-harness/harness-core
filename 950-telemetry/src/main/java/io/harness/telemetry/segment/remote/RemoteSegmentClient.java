package io.harness.telemetry.segment.remote;

import io.harness.telemetry.remote.TelemetryDataDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RemoteSegmentClient {
  String SEGMENT_URI = "harness";

  @POST(SEGMENT_URI) Call<TelemetryDataDTO> reportEvent(@Body TelemetryDataDTO segmentDataDTO);
}
