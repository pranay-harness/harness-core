package software.wings.resources.secretsmanagement;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import io.harness.secretmanagerclient.dto.SecretTextCreateDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.security.encryption.EncryptedDataDetail;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.NGSecretService;
import software.wings.settings.SettingVariableTypes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

@Api("secrets")
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
public class SecretsResourceNG {
  private final NgManagerServiceDriver ngManagerServiceDriver;
  private final NGSecretService ngSecretService;
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  public static final String ORG_IDENTIFIER = "orgIdentifier";
  public static final String PROJECT_IDENTIFIER = "projectIdentifier";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";

  @Inject
  public SecretsResourceNG(NgManagerServiceDriver ngManagerServiceDriver, NGSecretService ngSecretService) {
    this.ngManagerServiceDriver = ngManagerServiceDriver;
    this.ngSecretService = ngSecretService;
  }

  @POST
  public RestResponse<EncryptedDataDTO> createSecret(SecretTextCreateDTO dto) {
    EncryptedData encryptedData = EncryptedDataMapper.fromDTO(dto);
    EncryptedData createdEncryptedData = ngSecretService.createSecretText(encryptedData, dto.getValue());
    return new RestResponse<>(EncryptedDataMapper.toDTO(createdEncryptedData));
  }

  @GET
  public RestResponse<PageResponse<EncryptedDataDTO>> listSecrets(
      @QueryParam(ACCOUNT_IDENTIFIER) final String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) final String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER) final String projectIdentifier,
      @QueryParam(LIMIT) @DefaultValue("100") final String limit,
      @QueryParam(OFFSET) @DefaultValue("0") final String offset, @QueryParam("type") final SettingVariableTypes type) {
    PageResponse<EncryptedData> encryptedDataPageResponse =
        ngSecretService.listSecrets(accountIdentifier, orgIdentifier, projectIdentifier, type, limit, offset);
    List<EncryptedDataDTO> dtoList =
        encryptedDataPageResponse.getResponse().stream().map(EncryptedDataMapper::toDTO).collect(Collectors.toList());
    PageResponse<EncryptedDataDTO> dtoPageResponse = new PageResponse<>();
    dtoPageResponse.setResponse(dtoList);
    dtoPageResponse.setTotal(encryptedDataPageResponse.getTotal());
    dtoPageResponse.setLimit(encryptedDataPageResponse.getLimit());
    dtoPageResponse.setOffset(encryptedDataPageResponse.getOffset());
    return new RestResponse<>(dtoPageResponse);
  }

  @GET
  @Path("{identifier}")
  public RestResponse<EncryptedDataDTO> get(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) final String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) final String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER) final String projectIdentifier) {
    Optional<EncryptedData> encryptedDataOptional =
        ngSecretService.getSecretText(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return new RestResponse<>(encryptedDataOptional.map(EncryptedDataMapper::toDTO).orElse(null));
  }

  @PUT
  @Path("{identifier}")
  public RestResponse<Boolean> updateSecret(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) final String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) final String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER) final String projectIdentifier, SecretTextUpdateDTO dto) {
    if (!StringUtils.isEmpty(dto.getPath()) && !StringUtils.isEmpty(dto.getValue())) {
      throw new InvalidRequestException("Cannot update both path and value", INVALID_REQUEST, USER);
    }
    Optional<EncryptedData> encryptedDataOptional =
        ngSecretService.getSecretText(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (encryptedDataOptional.isPresent()) {
      EncryptedData appliedUpdate = EncryptedDataMapper.applyUpdate(dto, encryptedDataOptional.get());
      boolean success = ngSecretService.updateSecretText(appliedUpdate, dto.getValue());
      return new RestResponse<>(success);
    }
    throw new InvalidRequestException("No such secret found", INVALID_REQUEST, USER);
  }

  @DELETE
  @Path("{identifier}")
  public RestResponse<Boolean> deleteSecret(@PathParam("identifier") String identifier,
      @QueryParam(ACCOUNT_IDENTIFIER) final String accountIdentifier,
      @QueryParam(ORG_IDENTIFIER) final String orgIdentifier,
      @QueryParam(PROJECT_IDENTIFIER) final String projectIdentifier) {
    return new RestResponse<>(
        ngSecretService.deleteSecretText(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @POST
  @Path("encryption-details")
  @Consumes("application/x-kryo")
  @Produces("application/x-kryo")
  public RestResponse<List<EncryptedDataDetail>> getEncryptionDetails(EncryptableSetting encryptableSetting) {
    return new RestResponse<>(ngSecretService.getEncryptionDetails(encryptableSetting));
  }

  @GET
  @Path("task")
  public RestResponse<Boolean> sendTaskResponse() {
    boolean sendTaskResultResponse = ngManagerServiceDriver.sendTaskResult(generateUuid(), null);
    return new RestResponse<>(sendTaskResultResponse);
  }
}
