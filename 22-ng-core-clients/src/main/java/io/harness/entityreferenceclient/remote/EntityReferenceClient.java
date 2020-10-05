package io.harness.entityreferenceclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EntityReferenceClient {
  String ENTITY_REFERENCE_API = "entityReference";

  @POST(ENTITY_REFERENCE_API) Call<ResponseDTO<EntityReferenceDTO>> save(@Body EntityReferenceDTO entityReferenceDTO);

  @DELETE(ENTITY_REFERENCE_API)
  Call<ResponseDTO<Boolean>> delete(
      @Query("referredEntityFQN") String referredEntityFQN, @Query("referredByEntityFQN") String referredByEntityFQN);

  @GET(ENTITY_REFERENCE_API + "/isEntityReferenced")
  Call<ResponseDTO<Boolean>> isEntityReferenced(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier);
}
