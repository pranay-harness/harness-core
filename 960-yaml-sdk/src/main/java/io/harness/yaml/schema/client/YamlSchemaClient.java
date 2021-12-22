package io.harness.yaml.schema.client;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(DX)
public interface YamlSchemaClient {
  @GET("partial-yaml-schema")
  Call<ResponseDTO<PartialSchemaDTO>> get(@Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Query("scope") Scope scope);
  @GET("partial-yaml-schema/details")
  Call<ResponseDTO<YamlSchemaDetailsWrapper>> getSchemaDetails(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Query("scope") Scope scope);
  @POST("partial-yaml-schema/merged")
  Call<ResponseDTO<PartialSchemaDTO>> get(@Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Query("scope") Scope scope,
      @Body YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper);
}
