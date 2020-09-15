package io.harness.ng.core.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.ORG_KEY;
import static io.harness.ng.NGConstants.PAGE_KEY;
import static io.harness.ng.NGConstants.PROJECT_KEY;
import static io.harness.ng.NGConstants.SEARCH_TERM_KEY;
import static io.harness.ng.NGConstants.SIZE_KEY;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.SecretType;
import io.harness.serializer.JsonUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.InputStream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/v2/secrets")
@Api("/v2/secrets")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretResourceV2 {
  private final SecretCrudService ngSecretService;

  @POST
  @Consumes({"application/json"})
  @ApiOperation(value = "Create a secret", nickname = "postSecret")
  public ResponseDTO<SecretDTOV2> create(
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @Valid SecretDTOV2 dto) {
    return ResponseDTO.newResponse(ngSecretService.create(accountIdentifier, dto));
  }

  @POST
  @Path("/validate")
  @Consumes({"application/json", "application/yaml"})
  @ApiOperation(value = "Validate a secret", nickname = "validateSecret")
  public ResponseDTO<SecretValidationResultDTO> validateSecret(
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier, @QueryParam(IDENTIFIER_KEY) String identifier,
      SecretValidationMetaData metadata) {
    return ResponseDTO.newResponse(
        ngSecretService.validateSecret(accountIdentifier, orgIdentifier, projectIdentifier, identifier, metadata));
  }

  @POST
  @Path("/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Create a secret via yaml", nickname = "postSecretViaYaml")
  public ResponseDTO<SecretDTOV2> createViaYaml(
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @Valid SecretDTOV2 dto) {
    return ResponseDTO.newResponse(ngSecretService.createViaYaml(accountIdentifier, dto));
  }

  @GET
  @ApiOperation(value = "Get secrets", nickname = "listSecretsV2")
  public ResponseDTO<NGPageResponse<SecretDTOV2>> list(@QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(ORG_KEY) String orgIdentifier, @QueryParam(PROJECT_KEY) String projectIdentifier,
      @QueryParam("type") SecretType secretType, @QueryParam(SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(PAGE_KEY) @DefaultValue("0") int page, @QueryParam(SIZE_KEY) @DefaultValue("100") int size) {
    return ResponseDTO.newResponse(
        ngSecretService.list(accountIdentifier, orgIdentifier, projectIdentifier, secretType, searchTerm, page, size));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets secret", nickname = "getSecretV2")
  public ResponseDTO<SecretDTOV2> get(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).orElse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete secret", nickname = "deleteSecretV2")
  public ResponseDTO<Boolean> delete(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @QueryParam(ORG_KEY) String orgIdentifier,
      @QueryParam(PROJECT_KEY) String projectIdentifier) {
    return ResponseDTO.newResponse(
        ngSecretService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret", nickname = "putSecret")
  @Consumes({"application/json"})
  public ResponseDTO<Boolean> updateSecret(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @Valid SecretDTOV2 dto) {
    return ResponseDTO.newResponse(ngSecretService.update(accountIdentifier, dto));
  }

  @PUT
  @Path("{identifier}/yaml")
  @Consumes({"application/yaml"})
  @ApiOperation(value = "Update a secret via yaml", nickname = "putSecretViaYaml")
  public ResponseDTO<Boolean> updateSecretViaYaml(@PathParam(IDENTIFIER_KEY) @NotEmpty String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier, @Valid SecretDTOV2 dto) {
    return ResponseDTO.newResponse(ngSecretService.updateViaYaml(accountIdentifier, dto));
  }

  @PUT
  @Path("files/{identifier}")
  @ApiOperation(value = "Update a secret file", nickname = "putSecretFileV2")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<Boolean> updateSecretFile(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @NotNull @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("spec") String spec) {
    SecretDTOV2 dto = JsonUtils.asObject(spec, SecretDTOV2.class);
    return ResponseDTO.newResponse(ngSecretService.updateFile(accountIdentifier, dto, uploadedInputStream));
  }

  @POST
  @Path("files")
  @ApiOperation(value = "Create a secret file", nickname = "postSecretFileV2")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<SecretDTOV2> createSecretFile(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_KEY) @NotNull String accountIdentifier,
      @NotNull @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("spec") String spec) {
    SecretDTOV2 dto = JsonUtils.asObject(spec, SecretDTOV2.class);
    return ResponseDTO.newResponse(ngSecretService.createFile(accountIdentifier, dto, uploadedInputStream));
  }
}
