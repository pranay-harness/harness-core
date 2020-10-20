package io.harness.connector.apis.client;

import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.ng.core.dto.ResponseDTO;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Optional;

public interface ConnectorResourceClient {
  String CONNECTORS_API = "/connectors";

  @GET(CONNECTORS_API + "/{connectorIdentifier}")
  Call<ResponseDTO<Optional<ConnectorDTO>>> get(@Path("connectorIdentifier") String connectorIdentifier,
      @NotEmpty @Query("accountIdentifier") String accountIdentifier, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);
}
