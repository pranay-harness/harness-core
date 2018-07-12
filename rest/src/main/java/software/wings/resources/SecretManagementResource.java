package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.data.structure.EmptyPredicate;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataParam;
import retrofit2.http.Body;
import software.wings.app.MainConfiguration;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.UuidAware;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.EncryptionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.JsonUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 10/30/17.
 */
@Api("secrets")
@Path("/secrets")
@Produces("application/json")
@Consumes("application/json")
@Scope(ResourceType.SETTING)
public class SecretManagementResource {
  @Inject private SecretManager secretManager;
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
  public RestResponse<Collection<UuidAware>> listEncryptedValues(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(secretManager.listEncryptedValues(accountId));
  }

  @GET
  @Path("/list-configs")
  @Timed
  @ExceptionMetered
  public RestResponse<List<EncryptionConfig>> listEncryptionConfig(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(secretManager.listEncryptionConfig(accountId));
  }

  @GET
  @Path("/transition-config")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> transitionSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("fromEncryptionType") EncryptionType fromEncryptionType, @QueryParam("fromKmsId") String fromKmsId,
      @QueryParam("toEncryptionType") EncryptionType toEncryptionType, @QueryParam("toKmsId") String toKmsId) {
    return new RestResponse<>(
        secretManager.transitionSecrets(accountId, fromEncryptionType, fromKmsId, toEncryptionType, toKmsId));
  }

  @POST
  @Path("/add-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveSecret(@QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.saveSecret(
        accountId, secretText.getName(), secretText.getValue(), secretText.getUsageRestrictions()));
  }

  @POST
  @Path("/add-local-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveSecretUsingLocalMode(
      @QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.saveSecretUsingLocalMode(
        accountId, secretText.getName(), secretText.getValue(), secretText.getUsageRestrictions()));
  }

  @POST
  @Path("/update-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> updateSecret(@QueryParam("accountId") final String accountId,
      @QueryParam("uuid") final String uuId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.updateSecret(
        accountId, uuId, secretText.getName(), secretText.getValue(), secretText.getUsageRestrictions()));
  }

  @DELETE
  @Path("/delete-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteSecret(
      @QueryParam("accountId") final String accountId, @QueryParam("uuid") final String uuId) {
    return new RestResponse<>(secretManager.deleteSecret(accountId, uuId));
  }

  @POST
  @Path("/add-file")
  @Timed
  @Consumes(MULTIPART_FORM_DATA)
  @ExceptionMetered
  public RestResponse<String> saveFile(@QueryParam("accountId") final String accountId,
      @FormDataParam("name") final String name, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString) {
    return new RestResponse<>(
        secretManager.saveFile(accountId, name, getUsageRestrictionsFromJson(usageRestrictionsString),
            new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit())));
  }

  private UsageRestrictions getUsageRestrictionsFromJson(String usageRestrictionsString) {
    // TODO use a bean param instead. It wasn't working for some reason.
    if (EmptyPredicate.isNotEmpty(usageRestrictionsString)) {
      try {
        return JsonUtils.asObject(usageRestrictionsString, UsageRestrictions.class);
      } catch (Exception ex) {
        throw new WingsException("Invalid usage restrictions");
      }
    }
    return null;
  }

  @POST
  @Path("/update-file")
  @Timed
  @Consumes(MULTIPART_FORM_DATA)
  @ExceptionMetered
  public RestResponse<Boolean> updateFile(@QueryParam("accountId") final String accountId,
      @FormDataParam("name") final String name,
      @FormDataParam("usageRestrictions") final String usageRestrictionsString,
      @FormDataParam("uuid") final String fileId, @FormDataParam("file") InputStream uploadedInputStream) {
    return new RestResponse<>(
        secretManager.updateFile(accountId, name, fileId, getUsageRestrictionsFromJson(usageRestrictionsString),
            new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit())));
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
      @QueryParam("currentEnvId") String currentEnvId, @BeanParam PageRequest<EncryptedData> pageRequest) {
    try {
      pageRequest.addFilter("type", Operator.EQ, type);
      return new RestResponse<>(secretManager.listSecrets(accountId, pageRequest, currentAppId, currentEnvId));
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
    return new RestResponse<>(secretManager.getSecretUsage(accountId, secretId));
  }
}
