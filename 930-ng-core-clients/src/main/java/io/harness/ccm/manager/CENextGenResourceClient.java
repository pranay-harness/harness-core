/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.manager;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.ng.core.dto.ResponseDTO;

import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(CE)
public interface CENextGenResourceClient {
  String BASE_API = "ccm/api";

  @GET(BASE_API + "/") Call<ResponseDTO<Boolean>> test();

  @POST("/ccm/api/testconnection")
  Call<ResponseDTO<ConnectorValidationResult>> testConnection(
      @NotEmpty @Query(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @Body ConnectorResponseDTO connectorResponseDTO);
}
