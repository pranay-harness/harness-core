package io.harness.entityactivity.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.POST;

@OwnedBy(DX)
public interface EntityActivityClient {
  String ACTIVITY_HISTORY_API = "activityHistory";

  @POST(ACTIVITY_HISTORY_API) Call<ResponseDTO<NGActivityDTO>> save(NGActivityDTO activityHistory);
}
