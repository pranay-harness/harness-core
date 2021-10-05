package io.harness.enforcement.client;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapRequestDTO;
import io.harness.enforcement.beans.internal.RestrictionMetadataMapResponseDTO;
import io.harness.enforcement.beans.metadata.FeatureRestrictionMetadataDTO;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.ng.core.dto.ResponseDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(HarnessTeam.GTM)
public interface EnforcementClient {
  @GET("/enforcement/{featureRestrictionName}/metadata")
  Call<ResponseDTO<FeatureRestrictionMetadataDTO>> getFeatureRestrictionMetadata(
      @Path("featureRestrictionName") FeatureRestrictionName featureRestrictionName,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier);

  @POST("/enforcement/metadata")
  Call<ResponseDTO<RestrictionMetadataMapResponseDTO>> getFeatureRestrictionMetadataMap(
      @Body RestrictionMetadataMapRequestDTO restrictionMetadataMapRequestDTO,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier);
}
