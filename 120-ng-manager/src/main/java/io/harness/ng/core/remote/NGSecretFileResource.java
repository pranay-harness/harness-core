package io.harness.ng.core.remote;

import static io.harness.NGConstants.FILE_KEY;
import static io.harness.NGConstants.SECRET_MANAGER_KEY;
import static io.harness.secretmanagerclient.SecretType.SecretFile;

import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
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

import software.wings.app.FileUploadLimit;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

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
      @NotNull @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @FormDataParam(NGCommonEntityConstants.TAGS_KEY) String tagsString,
      @NotNull @FormDataParam(NGCommonEntityConstants.NAME_KEY) String name,
      @NotNull @FormDataParam(NGCommonEntityConstants.ACCOUNT_KEY) String account,
      @NotNull @SecretTypeAllowedValues(allowedValues = {SecretFile}) @FormDataParam(NGResourceFilterConstants.TYPE_KEY)
      SecretType secretType, @FormDataParam(NGCommonEntityConstants.ORG_KEY) String org,
      @FormDataParam(NGCommonEntityConstants.PROJECT_KEY) String project,
      @NotNull @FormDataParam(SECRET_MANAGER_KEY) String secretManager,
      @NotNull @EntityIdentifier @FormDataParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @FormDataParam(NGCommonEntityConstants.DESCRIPTION_KEY) String description) {
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
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @NotEmpty String identifier, @Valid SecretFileDTO dto) {
    return ResponseDTO.newResponse(ngSecretFileService.update(dto, null));
  }
}
