/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.serializer.kryo.KryoResponse;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConnectorResourceClient {
  String CONNECTORS_API = "connectors";

  @GET(CONNECTORS_API + "/{connectorIdentifier}")
  Call<ResponseDTO<Optional<ConnectorDTO>>> get(@Path("connectorIdentifier") String connectorIdentifier,
      @NotEmpty @Query("accountIdentifier") String accountIdentifier, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);

  @POST(CONNECTORS_API + "/listbyfqn")
  Call<ResponseDTO<List<ConnectorResponseDTO>>> listConnectorByFQN(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier, @Body List<String> connectorsFQN);

  @GET(CONNECTORS_API + "/{identifier}/validation-params")
  @KryoResponse
  Call<ResponseDTO<ConnectorValidationParams>> getConnectorValidationParams(
      @Path(NGCommonEntityConstants.IDENTIFIER_KEY) String connectorIdentifier,
      @NotNull @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier);

  @POST(CONNECTORS_API + "/listV2")
  Call<ResponseDTO<PageResponse<ConnectorResponseDTO>>> listConnectors(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGResourceFilterConstants.PAGE_KEY) int page, @Query(NGResourceFilterConstants.SIZE_KEY) int size,
      @Body ConnectorFilterPropertiesDTO connectorListFilter,
      @Query("getDistinctFromBranches") Boolean getDistinctFromBranches);
}
