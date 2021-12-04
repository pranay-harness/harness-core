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
