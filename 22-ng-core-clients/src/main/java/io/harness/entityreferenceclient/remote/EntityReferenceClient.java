package io.harness.entityreferenceclient.remote;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EntityReferenceClient {
  String ENTITY_REFERENCE_API = "entityReference";

  @POST(ENTITY_REFERENCE_API) Call<ResponseDTO<EntityReferenceDTO>> save(@Body EntityReferenceDTO entityReferenceDTO);

  @DELETE(ENTITY_REFERENCE_API)
  Call<ResponseDTO<Boolean>> delete(
      @Query("referredEntityFQN") String referredEntityFQN, @Query("referredByEntityFQN") String referredByEntityFQN);
}
