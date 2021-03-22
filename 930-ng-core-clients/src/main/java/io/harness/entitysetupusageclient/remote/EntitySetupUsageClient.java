package io.harness.entitysetupusageclient.remote;

import static io.harness.NGConstants.REFERRED_BY_ENTITY_FQN;
import static io.harness.NGConstants.REFERRED_BY_ENTITY_TYPE;
import static io.harness.NGConstants.REFERRED_ENTITY_FQN;
import static io.harness.NGConstants.REFERRED_ENTITY_TYPE;

import io.harness.EntityType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entitysetupusage.dto.EntitySetupUsageDTO;

import java.util.List;
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
  String INTERNAL_ENTITY_REFERENCE_API = "entitySetupUsage/internal";

  @GET(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<Page<EntitySetupUsageDTO>>> listAllEntityUsage(@Query(NGResourceFilterConstants.PAGE_KEY) int page,
      @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN, @Query(REFERRED_ENTITY_TYPE) EntityType referredEntityType,
      @Query(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm);

  @GET(INTERNAL_ENTITY_REFERENCE_API + "/listAllReferredUsages")
  Call<ResponseDTO<List<EntitySetupUsageDTO>>> listAllReferredUsages(
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN,
      @Query(REFERRED_ENTITY_TYPE) EntityType referredEntityType,
      @Query(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm);

  @Deprecated
  @POST(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<EntitySetupUsageDTO>> save(@Body EntitySetupUsageDTO entitySetupUsageDTO);

  @Deprecated
  @DELETE(INTERNAL_ENTITY_REFERENCE_API)
  Call<ResponseDTO<Boolean>> delete(@NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN, @Query(REFERRED_ENTITY_TYPE) EntityType referredEntityType,
      @Query(REFERRED_BY_ENTITY_FQN) String referredByEntityFQN,
      @Query(REFERRED_BY_ENTITY_TYPE) EntityType referredByEntityType);

  @GET(INTERNAL_ENTITY_REFERENCE_API + "/isEntityReferenced")
  Call<ResponseDTO<Boolean>> isEntityReferenced(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(REFERRED_ENTITY_FQN) String referredEntityFQN, @Query(REFERRED_ENTITY_TYPE) EntityType referredEntityType);
}
