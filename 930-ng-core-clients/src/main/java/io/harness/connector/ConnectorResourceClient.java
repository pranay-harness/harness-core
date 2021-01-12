package io.harness.connector;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ResponseDTO;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.Call;
import retrofit2.http.*;

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
