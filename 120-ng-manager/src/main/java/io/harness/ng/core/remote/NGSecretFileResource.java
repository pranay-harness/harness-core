package io.harness.ng.core.remote;

import static io.harness.ng.NGConstants.ACCOUNT_KEY;
import static io.harness.ng.NGConstants.DESCRIPTION_KEY;
import static io.harness.ng.NGConstants.FILE_KEY;
import static io.harness.ng.NGConstants.IDENTIFIER_KEY;
import static io.harness.ng.NGConstants.NAME_KEY;
import static io.harness.ng.NGConstants.ORG_KEY;
import static io.harness.ng.NGConstants.PROJECT_KEY;
import static io.harness.ng.NGConstants.SECRET_MANAGER_KEY;
import static io.harness.ng.NGConstants.TAGS_KEY;
import static io.harness.ng.NGConstants.TYPE_KEY;
import static io.harness.secretmanagerclient.SecretType.SecretFile;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import com.google.inject.Inject;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.SecretTypeAllowedValues;
import io.harness.ng.core.api.NGSecretFileService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.FileUploadLimit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("secrets/files")
@Api("secrets/files")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretFileResource {
  private final NGSecretFileService ngSecretFileService;
  private final FileUploadLimit fileUploadLimit;

  @POST
  @Path("/yaml")
  @ApiOperation(value = "Create a secret file via yaml", nickname = "postSecretFileViaYaml")
  @Consumes({"application/yaml"})
  public ResponseDTO<EncryptedDataDTO> createViaYaml(@Valid SecretFileDTO dto) {
    return ResponseDTO.newResponse(toDTO(ngSecretFileService.create(dto, null)));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret file", nickname = "putSecretFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<Boolean> update(@PathParam("identifier") @NotEmpty String pathIdentifier,
      @NotNull @FormDataParam(FILE_KEY) InputStream uploadedInputStream, @FormDataParam(TAGS_KEY) String tagsString,
      @NotNull @FormDataParam(NAME_KEY) String name, @NotNull @FormDataParam(ACCOUNT_KEY) String account,
      @NotNull @SecretTypeAllowedValues(allowedValues = {SecretFile}) @FormDataParam(TYPE_KEY) SecretType secretType,
      @FormDataParam(ORG_KEY) String org, @FormDataParam(PROJECT_KEY) String project,
      @NotNull @FormDataParam(SECRET_MANAGER_KEY) String secretManager,
      @NotNull @EntityIdentifier @FormDataParam(IDENTIFIER_KEY) String identifier,
      @FormDataParam(DESCRIPTION_KEY) String description) {
    List<String> tags = new ArrayList<>();
    if (!StringUtils.isEmpty(tagsString)) {
      tags = Arrays.asList(tagsString.trim().split("\\s*,\\s*"));
    }
    SecretFileDTO dto = SecretFileDTO.builder()
                            .account(account)
                            .org(org)
                            .project(project)
                            .identifier(identifier)
                            .secretManager(secretManager)
                            .description(description)
                            .name(name)
                            .tags(tags)
                            .type(secretType)
                            .build();
    return ResponseDTO.newResponse(ngSecretFileService.update(
        dto, new BoundedInputStream(uploadedInputStream, fileUploadLimit.getEncryptedFileLimit())));
  }

  @PUT
  @Path("{identifier}/yaml")
  @ApiOperation(value = "Update a secret file via yaml", nickname = "putSecretFileViaYaml")
  @Consumes({"application/yaml"})
  public ResponseDTO<Boolean> updateSecretFileViaYaml(
      @PathParam(IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretFileDTO dto) {
    return ResponseDTO.newResponse(ngSecretFileService.update(dto, null));
  }
}
