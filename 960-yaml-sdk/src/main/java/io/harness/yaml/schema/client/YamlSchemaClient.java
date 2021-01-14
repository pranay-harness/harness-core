package io.harness.yaml.schema.client;

import io.harness.NGCommonEntityConstants;
import io.harness.encryption.Scope;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.yaml.schema.beans.PartialSchemaDTO;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface YamlSchemaClient {
  @GET("yaml-schema")
  Call<ResponseDTO<PartialSchemaDTO>> get(@Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier, @Query("scope") Scope scope);
}
