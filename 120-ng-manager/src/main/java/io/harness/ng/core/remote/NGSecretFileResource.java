package io.harness.ng.core.remote;

import static io.harness.secretmanagerclient.NGConstants.ACCOUNT_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.DESCRIPTION_KEY;
import static io.harness.secretmanagerclient.NGConstants.FILE_KEY;
import static io.harness.secretmanagerclient.NGConstants.IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.NAME_KEY;
import static io.harness.secretmanagerclient.NGConstants.ORG_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.PROJECT_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.SECRET_MANAGER_IDENTIFIER_KEY;
import static io.harness.secretmanagerclient.NGConstants.TAGS_KEY;
import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import com.google.inject.Inject;

import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretFileService;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.FileUploadLimit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("secret-files")
@Api("secret-files")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
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
  @ApiOperation(value = "Create a secret file", nickname = "createSecretFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<EncryptedDataDTO> create(@NotNull @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @FormDataParam(TAGS_KEY) String tagsString, @NotNull @FormDataParam(NAME_KEY) String name,
      @NotNull @FormDataParam(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier,
      @FormDataParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @FormDataParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier,
      @NotNull @FormDataParam(SECRET_MANAGER_IDENTIFIER_KEY) String secretManagerIdentifier,
      @NotNull @EntityIdentifier @FormDataParam(IDENTIFIER_KEY) String identifier,
      @FormDataParam(DESCRIPTION_KEY) String description) {
    List<String> tags = new ArrayList<>();
    if (!StringUtils.isEmpty(tagsString)) {
      tags = Arrays.asList(tagsString.trim().split(","));
    }
    return ResponseDTO.newResponse(toDTO(ngSecretFileService.create(accountIdentifier, orgIdentifier, projectIdentifier,
        identifier, secretManagerIdentifier, name, description, tags,
        new BoundedInputStream(uploadedInputStream, fileUploadLimit.getEncryptedFileLimit()))));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a secret file", nickname = "updateSecretFile")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public ResponseDTO<Boolean> update(@PathParam(IDENTIFIER_KEY) String identifier,
      @NotNull @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @NotNull @FormDataParam(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier,
      @FormDataParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @FormDataParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier,
      @FormDataParam(DESCRIPTION_KEY) String description, @FormDataParam(TAGS_KEY) String tagsString) {
    List<String> tags = new ArrayList<>();
    if (!StringUtils.isEmpty(tagsString)) {
      tags = Arrays.asList(tagsString.trim().split(","));
    }
    return ResponseDTO.newResponse(
        ngSecretFileService.update(accountIdentifier, orgIdentifier, projectIdentifier, identifier, description, tags,
            new BoundedInputStream(uploadedInputStream, fileUploadLimit.getEncryptedFileLimit())));
  }
}
