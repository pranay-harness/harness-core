package io.harness.entitysetupusageclient.remote;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * All this apis are internal and won't be exposed to the customers. The APIs takes the FQN as input, FQN is fully
 * qualified Name of the entity. It is the unique key with which we can identify the resource.
 * for eg: For a project level connector it will be
 *      accountIdentifier/orgIdentifier/projectIdentifier/identifier
 *  For a input set it will be
 *    accountIdentifier/orgIdentifier/projectIdentifier/pipelineIdentifier/identifier
 */
public interface EntitySetupUsageClient {
  String REFERRED_ENTITY_FQN = "referredEntityFQN";
  String REFERRED_BY_ENTITY_FQN = "referredByEntityFQN";
  String INTERNAL_ENTITY_REFERENCE_API = "entitySetupUsage/internal";

  @GET(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<Page<EntitySetupUsageDTO>>> listAllEntityUsage(@Query(NGResourceFilterConstants.PAGE_KEY) int page,
      @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN,
      @Query(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm);

  @POST(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<EntitySetupUsageDTO>> save(@Body EntitySetupUsageDTO entitySetupUsageDTO);

  @DELETE(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<Boolean>> delete(@NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN, @Query(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN);

  @GET(INTERNAL_ENTITY_REFERENCE_API + "/isEntityReferenced")
  Call<ResponseDTO<Boolean>> isEntityReferenced(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN);

  @DELETE(INTERNAL_ENTITY_REFERENCE_API + "/deleteAllReferredByRecords")
  Call<ResponseDTO<Boolean>> deleteAllReferredByEntityRecords(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN);
}
