package software.wings.resources.secretsmanagement;

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

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.data.validator.EntityIdentifier;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.NGEncryptedDataMetadata;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.serializer.JsonUtils;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.FileUploadLimit;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.NGSecretFileService;
import software.wings.service.intfc.security.NGSecretService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("secret-files")
@Path("/ng/secret-files")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
public class SecretFilesResourceNG {
  private final NGSecretFileService ngSecretFileService;
  private final FileUploadLimit fileUploadLimits;
  private final NGSecretService ngSecretService;

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public RestResponse<EncryptedDataDTO> create(@NotNull @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @FormDataParam(TAGS_KEY) String tagsString, @NotNull @FormDataParam(NAME_KEY) String name,
      @NotNull @FormDataParam(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier,
      @FormDataParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @FormDataParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier,
      @NotNull @EntityIdentifier @FormDataParam(IDENTIFIER_KEY) String identifier,
      @NotNull @FormDataParam(SECRET_MANAGER_IDENTIFIER_KEY) String secretManagerIdentifier,
      @FormDataParam(DESCRIPTION_KEY) String description) {
    List<String> tags = new ArrayList<>();
    if (!StringUtils.isEmpty(tagsString)) {
      tags = JsonUtils.asObject(tagsString, new TypeReference<List<String>>() {});
    }
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(name)
                                      .ngMetadata(NGEncryptedDataMetadata.builder()
                                                      .accountIdentifier(accountIdentifier)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .identifier(identifier)
                                                      .secretManagerIdentifier(secretManagerIdentifier)
                                                      .description(description)
                                                      .tags(tags)
                                                      .build())
                                      .build();
    EncryptedData savedData = ngSecretFileService.create(
        encryptedData, new BoundedInputStream(uploadedInputStream, fileUploadLimits.getEncryptedFileLimit()));
    return new RestResponse<>(toDTO(savedData));
  }

  @PUT
  @Path("{identifier}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public RestResponse<Boolean> update(@PathParam(IDENTIFIER_KEY) String identifier,
      @FormDataParam(FILE_KEY) InputStream uploadedInputStream,
      @FormDataParam(ACCOUNT_IDENTIFIER_KEY) String accountIdentifier,
      @FormDataParam(ORG_IDENTIFIER_KEY) String orgIdentifier,
      @FormDataParam(PROJECT_IDENTIFIER_KEY) String projectIdentifier,
      @FormDataParam(DESCRIPTION_KEY) String description, @FormDataParam(TAGS_KEY) String tagsString) {
    List<String> tags = new ArrayList<>();
    if (!StringUtils.isEmpty(tagsString)) {
      tags = JsonUtils.asObject(tagsString, new TypeReference<List<String>>() {});
    }
    Optional<EncryptedData> encryptedDataOptional =
        ngSecretService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (encryptedDataOptional.isPresent()) {
      EncryptedData encryptedData = encryptedDataOptional.get();
      if (!Optional.ofNullable(encryptedData.getNgMetadata()).isPresent()) {
        encryptedData.setNgMetadata(NGEncryptedDataMetadata.builder().build());
      }
      encryptedData.getNgMetadata().setDescription(description);
      encryptedData.getNgMetadata().setTags(tags);
      return new RestResponse<>(ngSecretFileService.update(encryptedDataOptional.get(),
          new BoundedInputStream(uploadedInputStream, fileUploadLimits.getEncryptedFileLimit())));
    }
    throw new InvalidRequestException("No such file found.", WingsException.USER);
  }
}
