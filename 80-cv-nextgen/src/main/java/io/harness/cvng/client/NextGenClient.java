package io.harness.cvng.client;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorRequestDTO;
import io.harness.ng.core.dto.ResponseDTO;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Optional;

public interface NextGenClient {
  String CONNECTOR_BASE_PATH = "/accounts/{accountIdentifier}/connectors";

  @POST(CONNECTOR_BASE_PATH)
  Call<ResponseDTO<ConnectorDTO>> create(
      @Body ConnectorRequestDTO connectorRequestDTO, @Path("accountIdentifier") String accountIdentifier);

  @GET(CONNECTOR_BASE_PATH + "/{connectorIdentifier}")
  Call<ResponseDTO<Optional<ConnectorDTO>>> get(@Path("accountIdentifier") String accountIdentifier,
      @Path("connectorIdentifier") String connectorIdentifier, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);
}
