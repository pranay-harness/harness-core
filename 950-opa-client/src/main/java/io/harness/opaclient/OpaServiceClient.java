package io.harness.opaclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public interface OpaServiceClient {
  String API_PREFIX = "api/v1/";

  @POST(API_PREFIX + "evaluate-by-type")
  Call<OpaEvaluationResponseHolder> evaluateWithCredentials(@Query("type") String type,
      @Query("accountIdentifier") String accountIdentifier, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier, @Query("action") String action,
      @Query("entity") String entity, @Query("entityMetadata") String entityMetadata, @Body Object context);
}
