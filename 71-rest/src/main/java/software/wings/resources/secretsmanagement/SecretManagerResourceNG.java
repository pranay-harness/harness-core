package software.wings.resources.secretsmanagement;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigDTOConverter;
import io.harness.secretmanagerclient.dto.NGSecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.swagger.annotations.Api;
import software.wings.beans.SecretManagerConfig;
import software.wings.resources.secretsmanagement.mappers.SecretManagerConfigMapper;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.security.NGSecretManagerService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public static final String ORG_IDENTIFIER = "orgIdentifier";
  public static final String PROJECT_IDENTIFIER = "projectIdentifier";
  public static final String IDENTIFIER = "identifier";

  @POST
  public RestResponse<SecretManagerConfig> createSecretManager(SecretManagerConfigDTO dto) {
    SecretManagerConfig secretManagerConfig = SecretManagerConfigMapper.fromDTO(dto);
    return new RestResponse<>(ngSecretManagerService.createSecretManager(secretManagerConfig));
  }

  @GET
  public RestResponse<List<SecretManagerConfigDTO>> getSecretManagers(
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    List<SecretManagerConfig> secretManagerConfigs =
        ngSecretManagerService.listSecretManagers(accountIdentifier, orgIdentifier, projectIdentifier);
    return new RestResponse<>(
        secretManagerConfigs.stream().map(NGSecretManagerConfigDTOConverter::toDTO).collect(Collectors.toList()));
  }

  @GET
  @Path("{identifier}")
  public RestResponse<SecretManagerConfigDTO> getSecretManager(@PathParam(IDENTIFIER) String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return new RestResponse<>(secretManagerConfigOptional.map(SecretManagerConfig::toDTO).orElse(null));
  }

  @PUT
  @Path("/{identifier}")
  public RestResponse<SecretManagerConfig> updateSecretManager(@PathParam(IDENTIFIER) String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) @NotNull String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) String orgIdentifier, @QueryParam(PROJECT_IDENTIFIER) String projectIdentifier,
      NGSecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.getSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (secretManagerConfigOptional.isPresent()) {
      SecretManagerConfig applyUpdate =
          SecretManagerConfigMapper.applyUpdate(secretManagerConfigOptional.get(), secretManagerConfigUpdateDTO);
      return new RestResponse<>(ngSecretManagerService.updateSecretManager(applyUpdate));
    }
    throw new SecretManagementException(ErrorCode.SECRET_MANAGEMENT_ERROR, "Secret Manager not found", USER);
  }

  @DELETE
  @Path("/{identifier}")
  public RestResponse<Boolean> deleteSecretManager(@QueryParam("accountIdentifier") String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @PathParam("identifier") String identifier) {
    return new RestResponse<>(
        ngSecretManagerService.deleteSecretManager(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }
}
