package software.wings.resources.secretsmanagement;

import io.harness.NGCommonEntityConstants;
import io.harness.beans.SecretManagerConfig;
import io.harness.connector.ConnectorValidationResult;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.SecretManagerConfigUpdateDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataDTO;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.service.intfc.security.NGSecretManagerService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
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

@Api(value = "secret-managers", hidden = true)
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
    return new RestResponse<>(ngSecretManagerService.create(secretManagerConfig).toDTO(true));
  }

  @POST
  @Path("meta-data")
  @Produces("application/json")
  @Consumes("application/json")
  public RestResponse<SecretManagerMetadataDTO> getMetadata(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      SecretManagerMetadataRequestDTO requestDTO) {
    return new RestResponse<>(ngSecretManagerService.getMetadata(accountIdentifier, requestDTO));
  }

  @GET
  @Path("{identifier}/validate")
  public RestResponse<ConnectorValidationResult> validateSecretManager(@PathParam("identifier") String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String account,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String org,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String project) {
    return new RestResponse<>(ngSecretManagerService.testConnection(account, org, project, identifier));
  }

  @GET
  public RestResponse<List<SecretManagerConfigDTO>> getSecretManagers(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    List<SecretManagerConfig> secretManagerConfigs =
        ngSecretManagerService.list(accountIdentifier, orgIdentifier, projectIdentifier);
    List<SecretManagerConfigDTO> dtoList = new ArrayList<>();
    secretManagerConfigs.forEach(secretManagerConfig -> dtoList.add(secretManagerConfig.toDTO(true)));
    return new RestResponse<>(dtoList);
  }

  @GET
  @Path("{identifier}")
  public RestResponse<SecretManagerConfigDTO> getSecretManager(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    Optional<SecretManagerConfig> secretManagerConfigOptional =
        ngSecretManagerService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier, true);
    SecretManagerConfigDTO dto = null;
    if (secretManagerConfigOptional.isPresent()) {
      dto = secretManagerConfigOptional.get().toDTO(true);
    }
    return new RestResponse<>(dto);
  }

  @GET
  @Path("global/{accountIdentifier}")
  public RestResponse<SecretManagerConfigDTO> getGlobalSecretManager(
      @PathParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {
    return new RestResponse<>(ngSecretManagerService.getGlobalSecretManager(accountIdentifier).toDTO(false));
  }

  @PUT
  @Path("/{identifier}")
  @Produces("application/json")
  @Consumes("application/x-kryo")
  public RestResponse<SecretManagerConfigDTO> updateSecretManager(
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      SecretManagerConfigUpdateDTO secretManagerConfigUpdateDTO) {
    return new RestResponse<>(
        ngSecretManagerService
            .update(accountIdentifier, orgIdentifier, projectIdentifier, identifier, secretManagerConfigUpdateDTO)
            .toDTO(true));
  }

  @DELETE
  @Path("/{identifier}")
  public RestResponse<Boolean> deleteSecretManager(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    return new RestResponse<>(
        ngSecretManagerService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier, true));
  }
}
