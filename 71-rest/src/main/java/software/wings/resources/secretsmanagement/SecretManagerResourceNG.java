package software.wings.resources.secretsmanagement;

import static io.harness.exception.WingsException.USER;
import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.ORG_KEY;
import static io.harness.ng.NGConstants.PROJECT_KEY;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.swagger.annotations.Api;
import software.wings.beans.SecretManagerConfig;
import software.wings.resources.secretsmanagement.mappers.SecretManagerConfigMapper;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.security.NGSecretManagerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secret-managers")
@Path("/ng/secret-managers")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
public class SecretManagerResourceNG {
  @Inject private NGSecretManagerService ngSecretManagerService;

  @POST
  @Produces("application/json")
  @Consumes("application/x-kryo")
  public RestResponse<SecretManagerConfigDTO> createSecretManager(SecretManagerConfigDTO dto) {
    SecretManagerConfig secretManagerConfig = SecretManagerConfigMapper.fromDTO(dto);
    return new RestResponse<>(ngSecretManagerService.createSecretManager(secretManagerConfig).toDTO(true));
  }

  @GET
  public RestResponse<List<SecretManagerConfigDTO>> getSecretManagers(
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    List<SecretManagerConfig> secretManagerConfigs =
        ngSecretManagerService.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier);
    List<SecretManagerConfigDTO> dtoList = new ArrayList<>();
    secretManagerConfigs.forEach(secretManagerConfig -> dtoList.add(secretManagerConfig.toDTO(true)));
    return new RestResponse<>(dtoList);
  }

  @GET
  @Path("{identifier}")
  public RestResponse<SecretManagerConfigDTO> getSecretManager(@PathParam(IDENTIFIER_KEY) String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    SecretManagerConfigDTO dto = null;
    if (secretManagerConfigOptional.isPresent()) {
      dto = secretManagerConfigOptional.get().toDTO(true);
    }
    return new RestResponse<>(dto);
  }

  @GET
  @Path("global/{accountIdentifier}")
  public RestResponse<SecretManagerConfigDTO> getGlobalSecretManager(@PathParam(ACCOUNT_KEY) String accountIdentifier) {
    return new RestResponse<>(ngSecretManagerService.getGlobalSecretManager(accountIdentifier).toDTO(false));
  }

  @PUT
  @Path("/{identifier}")
  @Produces("application/json")
  @Consumes("application/x-kryo")
  public RestResponse<SecretManagerConfigDTO> updateSecretManager(@PathParam(IDENTIFIER_KEY) String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretManagerConfigOptional.isPresent()) {
      SecretManagerConfig applyUpdate =
          SecretManagerConfigMapper.applyUpdate(secretManagerConfigOptional.get(), secretManagerConfigUpdateDTO);
      return new RestResponse<>(ngSecretManagerService.updateSecretManager(applyUpdate).toDTO(true));
    }
    throw new SecretManagementException(ErrorCode.SECRET_MANAGEMENT_ERROR, "Secret Manager not found", USER);
  }

  @DELETE
  @Path("/{identifier}")
  public RestResponse<Boolean> deleteSecretManager(@QueryParam(ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @PathParam(IDENTIFIER_KEY) String identifier) {
    return new RestResponse<>(
        ngSecretManagerService.deleteSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
