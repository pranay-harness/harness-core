package io.harness.connector;

import io.harness.NGCommonEntityConstants;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;
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
}
