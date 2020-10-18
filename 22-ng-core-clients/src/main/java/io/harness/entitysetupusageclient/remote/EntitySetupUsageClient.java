package io.harness.entitysetupusageclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface EntitySetupUsageClient {
  static final String IS_REFERRED_ENTITY = "IsReferredEntity";
  String ENTITY_REFERENCE_API = "entitySetupUsage";

  @POST(ENTITY_REFERENCE_API)
  Call<ResponseDTO<EntitySetupUsageDTO>> save(@Body EntitySetupUsageDTO entitySetupUsageDTO);

  @DELETE(ENTITY_REFERENCE_API)
  Call<ResponseDTO<Boolean>> delete(@NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @Query(IS_REFERRED_ENTITY) Boolean isReferredEntity);

  @GET(ENTITY_REFERENCE_API + "/isEntityReferenced")
  Call<ResponseDTO<Boolean>> isEntityReferenced(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier);
}
