package software.wings.resources.secretsmanagement;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.service.intfc.security.NGSecretService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Api(value = "secrets", hidden = true)
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
public class SecretsResourceNG {
  private final NGSecretService ngSecretService;

  @Inject
  public SecretsResourceNG(NGSecretService ngSecretService) {
    this.ngSecretService = ngSecretService;
  }

  @DELETE
  @Path("{identifier}")
  public RestResponse<Boolean> deleteSecret(@PathParam("identifier") String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) final String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) final String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) final String projectIdentifier) {
    return new RestResponse<>(
        ngSecretService.deleteSecretText(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Path("migration/{identifier}")
  public RestResponse<EncryptedDataMigrationDTO> getEncryptedDataMigrationDTO(
      @PathParam("identifier") String identifier,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) final String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) final String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) final String projectIdentifier) {
    Optional<EncryptedDataMigrationDTO> encryptedDataOptional =
        ngSecretService.getEncryptedDataMigrationDTO(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return new RestResponse<>(encryptedDataOptional.orElse(null));
  }
}
