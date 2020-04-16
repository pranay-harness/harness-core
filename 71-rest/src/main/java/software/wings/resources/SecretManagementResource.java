package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.UuidAware;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;
import retrofit2.http.Body;
import software.wings.app.MainConfiguration;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Created by rsingh on 10/30/17.
 */
@Api("secrets")
@Path("/secrets")
@Produces("application/json")
@Consumes("application/json")
@Scope(ResourceType.SETTING)
@Slf4j
public class SecretManagementResource {
  @Inject private SecretManager secretManager;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private MainConfiguration configuration;

  @GET
  @Path("/usage")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<SecretUsageLog>> getUsageLogs(@BeanParam PageRequest<SecretUsageLog> pageRequest,
      @QueryParam("accountId") final String accountId, @QueryParam("entityId") final String entityId,
      @QueryParam("type") final SettingVariableTypes variableType) throws IllegalAccessException {
    return new RestResponse<>(secretManager.getUsageLogs(pageRequest, accountId, entityId, variableType));
  }

  @GET
  @Path("/change-logs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SecretChangeLog>> getChangeLogs(@QueryParam("accountId") final String accountId,
      @QueryParam("entityId") final String entityId, @QueryParam("type") final SettingVariableTypes variableType)
      throws IllegalAccessException {
    return new RestResponse<>(secretManager.getChangeLogs(accountId, entityId, variableType));
  }

  @GET
  @Path("/list-values")
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<SettingAttribute>> listEncryptedSettingAttributes(
      @QueryParam("accountId") final String accountId, @QueryParam("category") String category) {
    if (isEmpty(category)) {
      return new RestResponse<>(secretManager.listEncryptedSettingAttributes(accountId));
    } else {
      return new RestResponse<>(
          secretManager.listEncryptedSettingAttributes(accountId, Sets.newHashSet(category.toUpperCase())));
    }
  }

  @GET
  @Path("/list-configs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SecretManagerConfig>> listEncryptionConfig(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(secretManager.listSecretManagers(accountId));
  }

  @GET
  @Path("/get-config")
  @Timed
  @ExceptionMetered
  public RestResponse<SecretManagerConfig> getEncryptionConfig(@QueryParam("accountId") final String accountId,
      @QueryParam("secretsManagerConfigId") final String secretsManagerConfigId) {
    return new RestResponse<>(secretManager.getSecretManager(accountId, secretsManagerConfigId));
  }

  @GET
  @Path("/transition-config")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> transitionSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("fromEncryptionType") EncryptionType fromEncryptionType, @QueryParam("fromKmsId") String fromKmsId,
      @QueryParam("toEncryptionType") EncryptionType toEncryptionType, @QueryParam("toKmsId") String toKmsId) {
    return new RestResponse<>(
        secretManager.transitionSecrets(accountId, fromEncryptionType, fromKmsId, toEncryptionType, toKmsId));
  }

  @POST
  @Path("/import-secrets")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  @ExceptionMetered
  public RestResponse<List<String>> importSecrets(
      @QueryParam("accountId") final String accountId, @FormDataParam("file") final InputStream uploadInputStream) {
    return new RestResponse<>(secretManager.importSecretsViaFile(accountId, uploadInputStream));
  }

  @POST
  @Path("/add-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveSecret(@QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Adding a secret");
      return new RestResponse<>(secretManager.saveSecret(accountId, secretText.getKmsId(), secretText.getName(),
          secretText.getValue(), secretText.getPath(), secretText.getUsageRestrictions()));
    }
  }

  @POST
  @Path("/add-local-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveSecretUsingLocalMode(
      @QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.saveSecretUsingLocalMode(accountId, secretText.getName(),
        secretText.getValue(), secretText.getPath(), secretText.getUsageRestrictions()));
  }

  @POST
  @Path("/update-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateSecret(@QueryParam("accountId") final String accountId,
      @QueryParam("uuid") final String uuId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.updateSecret(accountId, uuId, secretText.getName(), secretText.getValue(),
        secretText.getPath(), secretText.getUsageRestrictions()));
  }

  @DELETE
  @Path("/delete-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteSecret(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String uuId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting a secret");
      return new RestResponse<>(secretManager.deleteSecret(accountId, uuId));
    }
  }

  @POST
  @Path("/add-file")
  @Timed
  @Consumes(MULTIPART_FORM_DATA)
  @ExceptionMetered
  public RestResponse<String> saveFile(@Context HttpServletRequest request,
      @QueryParam("accountId") final String accountId, @Nullable @FormDataParam("kmsId") final String kmsId,
      @FormDataParam("name") final String name, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString) {
    return new RestResponse<>(secretManager.saveFile(accountId, kmsId, name, request.getContentLengthLong(),
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit())));
  }

  @POST
  @Path("/update-file")
  @Timed
  @Consumes(MULTIPART_FORM_DATA)
  @ExceptionMetered
  public RestResponse<Boolean> updateFile(@Context HttpServletRequest request,
      @QueryParam("accountId") final String accountId, @FormDataParam("name") final String name,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString,
      @FormDataParam("uuid") final String fileId, @FormDataParam("file") InputStream uploadedInputStream) {
    // HAR-9736: If the user doesn't make any change in the secret file update, null is expected for now.
    if (uploadedInputStream == null) {
      // fill in with an empty input stream
      uploadedInputStream = new ByteArrayInputStream(new byte[0]);
    }
    return new RestResponse<>(secretManager.updateFile(accountId, name, fileId, request.getContentLengthLong(),
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit())));
  }

  @DELETE
  @Path("/delete-file")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteFile(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String uuId) {
    return new RestResponse<>(secretManager.deleteFile(accountId, uuId));
  }

  @GET
  @Path("/list-secrets-page")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<EncryptedData>> listSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("type") final SettingVariableTypes type, @QueryParam("currentAppId") String currentAppId,
      @QueryParam("currentEnvId") String currentEnvId, @DefaultValue("true") @QueryParam("details") boolean details,
      @BeanParam PageRequest<EncryptedData> pageRequest) {
    try {
      pageRequest.addFilter("type", Operator.EQ, type);
      pageRequest.addFilter("accountId", Operator.EQ, accountId);
      return new RestResponse<>(secretManager.listSecrets(accountId, pageRequest, currentAppId, currentEnvId, details));
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @GET
  @Path("/list-account-secrets")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<EncryptedData>> listSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("type") final SettingVariableTypes type, @DefaultValue("true") @QueryParam("details") boolean details,
      @BeanParam PageRequest<EncryptedData> pageRequest) {
    try {
      pageRequest.addFilter("type", Operator.EQ, type);
      pageRequest.addFilter("accountId", Operator.EQ, accountId);
      return new RestResponse<>(secretManager.listSecretsMappedToAccount(accountId, pageRequest, details));
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @GET
  @Path("/list-secret-usage")
  @Timed
  @ExceptionMetered
  public RestResponse<List<UuidAware>> getSecretUsage(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String secretId) {
    Set<SecretSetupUsage> setupUsages = secretManager.getSecretUsage(accountId, secretId);
    return new RestResponse<>(setupUsages.stream().map(SecretSetupUsage::getEntity).collect(Collectors.toList()));
  }

  @GET
  @Path("/list-setup-usage")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<SecretSetupUsage>> getSecretSetupUsage(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String secretId) {
    return new RestResponse<>(secretManager.getSecretUsage(accountId, secretId));
  }
}
