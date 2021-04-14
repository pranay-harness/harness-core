package io.harness.cvng.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;

import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
@OwnedBy(HarnessTeam.CV)
public interface NextGenClient {
  String CONNECTOR_BASE_PATH = "connectors";

  @POST(CONNECTOR_BASE_PATH)
  Call<ResponseDTO<ConnectorResponseDTO>> create(
      @Body ConnectorDTO connector, @Path("accountIdentifier") String accountIdentifier);

  @GET(CONNECTOR_BASE_PATH + "/{connectorIdentifier}")
  Call<ResponseDTO<ConnectorResponseDTO>> get(@Path("connectorIdentifier") String connectorIdentifier,
      @Query("accountIdentifier") String accountIdentifier, @Query("orgIdentifier") @NotNull String orgIdentifier,
      @Query("projectIdentifier") @NotNull String projectIdentifier);

  @GET("environments/{environmentIdentifier}")
  Call<ResponseDTO<EnvironmentResponseDTO>> getEnvironment(@Path("environmentIdentifier") String environmentIdentifier,
      @Query("accountId") String accountId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);

  @GET("services/{serviceIdentifier}")
  Call<ResponseDTO<ServiceResponseDTO>> getService(@Path("serviceIdentifier") String serviceIdentifier,
      @Query("accountId") String accountId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier);

  @GET("services")
  Call<ResponseDTO<PageResponse<ServiceResponseDTO>>> listServicesForProject(@Query("page") int page,
      @Query("size") int size, @Query("accountId") String accountId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier,
      @Query("serviceIdentifiers") Set<String> serviceIdentifiers);

  @GET("environments")
  Call<ResponseDTO<PageResponse<EnvironmentResponseDTO>>> listEnvironmentsForProject(@Query("page") int page,
      @Query("size") int size, @Query("accountId") String accountId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier, @Query("envIdentifiers") Set<String> envIdentifiers,
      @Query("sort") List<String> sort);

  @GET("projects/{projectIdentifier}")
  Call<ResponseDTO<ProjectResponse>> getProject(@Path("projectIdentifier") String projectIdentifier,
      @Query("accountIdentifier") String accountId, @Query("orgIdentifier") String orgIdentifier);
}
