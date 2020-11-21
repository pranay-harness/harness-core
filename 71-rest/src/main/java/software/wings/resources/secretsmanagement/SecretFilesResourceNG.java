package software.wings.resources.secretsmanagement;

import static io.harness.NGConstants.FILE_KEY;
import static io.harness.NGConstants.FILE_METADATA_KEY;

import static software.wings.resources.secretsmanagement.EncryptedDataMapper.toDTO;

import io.harness.NGCommonEntityConstants;
import io.harness.beans.EncryptedData;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretFileDTO;
import io.harness.secretmanagerclient.dto.SecretFileUpdateDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.serializer.JsonUtils;

import software.wings.app.FileUploadLimit;
import software.wings.service.intfc.security.NGSecretFileService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.InputStream;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Api("secret-files")
@Path("/ng/secret-files")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
public class SecretFilesResourceNG {
  private final NGSecretFileService ngSecretFileService;
  private final FileUploadLimit fileUploadLimits;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public RestResponse<EncryptedDataDTO> create(
      @FormDataParam(FILE_METADATA_KEY) String fileMetadata, @FormDataParam(FILE_KEY) InputStream inputStream) {
    if (StringUtils.isEmpty(fileMetadata)) {
      throw new InvalidRequestException("Meta data cannot be null/empty.");
    }
    SecretFileDTO dto = JsonUtils.asObject(fileMetadata, SecretFileDTO.class);
    EncryptedData savedData = ngSecretFileService.create(dto, inputStream);
    return new RestResponse<>(toDTO(savedData));
  }

  @PUT
  @Path("{identifier}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public RestResponse<Boolean> update(@PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @FormDataParam(FILE_METADATA_KEY) String fileMetadata) {
    if (StringUtils.isEmpty(fileMetadata)) {
      throw new InvalidRequestException("Meta data cannot be null/empty.");
    }
    SecretFileUpdateDTO dto = JsonUtils.asObject(fileMetadata, SecretFileUpdateDTO.class);
    return new RestResponse<>(ngSecretFileService.update(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier, dto, uploadedInputStream));
  }
}
