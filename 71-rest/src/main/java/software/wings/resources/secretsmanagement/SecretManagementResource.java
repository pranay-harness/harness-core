package software.wings.resources.secretsmanagement;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.UuidAware;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.JsonUtils;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.PUT;
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
  public RestResponse<PageResponse<SecretUsageLog>> getUsageLogs(@BeanParam PageRequest<SecretUsageLog> pageRequest,
      @QueryParam("accountId") final String accountId, @QueryParam("entityId") final String entityId,
      @QueryParam("type") final SettingVariableTypes variableType) throws IllegalAccessException {
    return new RestResponse<>(secretManager.getUsageLogs(pageRequest, accountId, entityId, variableType));
  }

  @GET
  @Path("/change-logs")
  public RestResponse<List<SecretChangeLog>> getChangeLogs(@QueryParam("accountId") final String accountId,
      @QueryParam("entityId") final String entityId, @QueryParam("type") final SettingVariableTypes variableType)
      throws IllegalAccessException {
    return new RestResponse<>(secretManager.getChangeLogs(accountId, entityId, variableType));
  }

  @GET
  @Path("/list-values")
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
  public RestResponse<List<SecretManagerConfig>> listEncryptionConfig(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(secretManager.listSecretManagers(accountId));
  }

  @GET
  @Path("/get-config")
  public RestResponse<SecretManagerConfig> getEncryptionConfig(@QueryParam("accountId") final String accountId,
      @QueryParam("secretsManagerConfigId") final String secretsManagerConfigId) {
    return new RestResponse<>(secretManager.getSecretManager(accountId, secretsManagerConfigId));
  }

  /*
   * Deprecated, use PUT /transition-config call instead of GET call
   * Templatized secret managers are not supported using this call.
   */
  @GET
  @Path("/transition-config")
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  @Deprecated
  public RestResponse<Boolean> transitionSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("fromEncryptionType") EncryptionType fromEncryptionType, @QueryParam("fromKmsId") String fromKmsId,
      @QueryParam("toEncryptionType") EncryptionType toEncryptionType, @QueryParam("toKmsId") String toKmsId) {
    return new RestResponse<>(secretManager.transitionSecrets(
        accountId, fromEncryptionType, fromKmsId, toEncryptionType, toKmsId, new HashMap<>(), new HashMap<>()));
  }

  @PUT
  @Path("/transition-config")
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> transitionSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("fromEncryptionType") EncryptionType fromEncryptionType, @QueryParam("fromKmsId") String fromKmsId,
      @QueryParam("toEncryptionType") EncryptionType toEncryptionType, @QueryParam("toKmsId") String toKmsId,
      @Body @NonNull Map<String, Map<String, String>> runtimeParameters) {
    return new RestResponse<>(secretManager.transitionSecrets(accountId, fromEncryptionType, fromKmsId,
        toEncryptionType, toKmsId, runtimeParameters.getOrDefault(fromKmsId, new HashMap<>()),
        runtimeParameters.getOrDefault(toKmsId, new HashMap<>())));
  }

  @POST
  @Path("/import-secrets")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public RestResponse<List<String>> importSecrets(
      @QueryParam("accountId") final String accountId, @FormDataParam("file") final InputStream uploadInputStream) {
    return new RestResponse<>(secretManager.importSecretsViaFile(accountId, uploadInputStream));
  }

  @POST
  @Path("/add-secret")
  public RestResponse<String> saveSecret(@QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Adding a secret");
      return new RestResponse<>(secretManager.saveSecret(accountId, secretText));
    }
  }

  @POST
  @Path("/add-local-secret")
  public RestResponse<String> saveSecretUsingLocalMode(
      @QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.saveSecretUsingLocalMode(accountId, secretText));
  }

  @POST
  @Path("/update-secret")
  public RestResponse<Boolean> updateSecret(@QueryParam("accountId") final String accountId,
      @QueryParam("uuid") final String uuid, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.updateSecret(accountId, uuid, secretText));
  }

  @DELETE
  @Path("/delete-secret")
  @Deprecated
  public RestResponse<Boolean> deleteSecret(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String uuId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting a secret");
      return new RestResponse<>(secretManager.deleteSecret(accountId, uuId, new HashMap<>()));
    }
  }

  /*
   * Ideally call for deleting a resource shouldn't use post method, but we need to get run time parameters
   * and UI libraries don't support body in a DELETE call. We could have sent run time parameters as query parameter,
   * but since these are credentials, there is a chance of these getting exposed in places like Nginx logs,
   * load balancer logs etc.
   */
  @POST
  @Path("/delete-secret")
  public RestResponse<Boolean> deleteSecret(@QueryParam("accountId") final String accountId,
      @QueryParam("uuid") final String uuId, @Body Map<String, String> runtimeParameters) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting a secret");
      return new RestResponse<>(secretManager.deleteSecret(accountId, uuId, runtimeParameters));
    }
  }

  @POST
  @Path("/add-file")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> saveFile(@Context HttpServletRequest request,
      @QueryParam("accountId") final String accountId, @Nullable @FormDataParam("kmsId") final String kmsId,
      @FormDataParam("name") final String name, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString,
      @FormDataParam("runtimeParameters") final String runtimeParametersString) {
    Map<String, String> runtimeParameters = new HashMap<>();
    if (!StringUtils.isEmpty(runtimeParametersString)) {
      runtimeParameters = JsonUtils.asObject(runtimeParametersString, new TypeReference<Map<String, String>>() {});
    }
    return new RestResponse<>(secretManager.saveFile(accountId, kmsId, name, request.getContentLengthLong(),
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit()),
        runtimeParameters));
  }

  @POST
  @Path("/update-file")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<Boolean> updateFile(@Context HttpServletRequest request,
      @QueryParam("accountId") final String accountId, @FormDataParam("name") final String name,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString,
      @FormDataParam("runtimeParameters") final String runtimeParametersString,
      @FormDataParam("uuid") final String fileId, @FormDataParam("file") InputStream uploadedInputStream) {
    // HAR-9736: If the user doesn't make any change in the secret file update, null is expected for now.
    if (uploadedInputStream == null) {
      // fill in with an empty input stream
      uploadedInputStream = new ByteArrayInputStream(new byte[0]);
    }
    Map<String, String> runtimeParameters = new HashMap<>();
    if (!StringUtils.isEmpty(runtimeParametersString)) {
      runtimeParameters = JsonUtils.asObject(runtimeParametersString, new TypeReference<Map<String, String>>() {});
    }
    return new RestResponse<>(secretManager.updateFile(accountId, name, fileId, request.getContentLengthLong(),
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit()),
        runtimeParameters));
  }

  /*
   * Deprecated, use POST call since this API requires runtimeParameters too
   */
  @DELETE
  @Path("/delete-file")
  @Deprecated
  public RestResponse<Boolean> deleteFile(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String uuId) {
    return new RestResponse<>(secretManager.deleteFile(accountId, uuId, new HashMap<>()));
  }

  @POST
  @Path("/delete-file")
  public RestResponse<Boolean> deleteFilePost(@QueryParam("accountId") final String accountId,
      @QueryParam("uuid") final String uuId, @Body Map<String, String> runtimeParameters) {
    return new RestResponse<>(secretManager.deleteFile(accountId, uuId, runtimeParameters));
  }

  @GET
  @Path("/list-secrets-page")
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
  public RestResponse<List<UuidAware>> getSecretUsage(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String secretId) {
    Set<SecretSetupUsage> setupUsages = secretManager.getSecretUsage(accountId, secretId);
    return new RestResponse<>(setupUsages.stream().map(SecretSetupUsage::getEntity).collect(Collectors.toList()));
  }

  @GET
  @Path("/list-setup-usage")
  public RestResponse<Set<SecretSetupUsage>> getSecretSetupUsage(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String secretId) {
    return new RestResponse<>(secretManager.getSecretUsage(accountId, secretId));
  }
}
