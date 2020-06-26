package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.dto.SecretDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/secrets")
@Api("/secrets")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
public class NGSecretManagerResource {
  private final SecretManager secretManager;

  @Inject
  public NGSecretManagerResource(SecretManager secretManager) {
    this.secretManager = secretManager;
  }

  @GET
  @Path("{secretId}")
  @ApiOperation(value = "Gets a secret by id", nickname = "getSecret")
  public SecretDTO get(
      @PathParam("secretId") @NotEmpty String secretId, @QueryParam("accountId") @NotBlank String accountId) {
    EncryptedData encryptedData = secretManager.getSecretById(accountId, secretId);
    return SecretDTO.builder().name(encryptedData.getName()).build();
  }
}
