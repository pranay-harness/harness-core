package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.apache.commons.lang3.StringUtils.trim;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.persistence.UuidAware;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataParam;
import retrofit2.http.Body;
import software.wings.app.MainConfiguration;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
  public RestResponse<Collection<UuidAware>> listEncryptedValues(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(secretManager.listEncryptedValues(accountId));
  }

  @GET
  @Path("/list-values-page")
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<UuidAware>> listEncryptedValues(@QueryParam("accountId") final String accountId,
      @QueryParam("type") final SettingVariableTypes type, @BeanParam PageRequest<EncryptedData> pageRequest) {
    pageRequest.addFilter("type", Operator.EQ, type);
    pageRequest.addFilter("accountId", Operator.EQ, accountId);
    return new RestResponse<>(secretManager.listEncryptedValues(accountId, pageRequest));
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
    List<SecretText> secretTexts = new ArrayList<>();
    InputStreamReader inputStreamReader = null;
    BufferedReader reader = null;
    try {
      inputStreamReader = new InputStreamReader(uploadInputStream, Charset.defaultCharset());
      reader = new BufferedReader(inputStreamReader);
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        String path = parts.length > 2 ? trim(parts[2]) : null;
        SecretText secretText = SecretText.builder().name(trim(parts[0])).value(trim(parts[1])).path(path).build();
        secretTexts.add(secretText);
      }
    } catch (IOException e) {
      throw new WingsException(e);
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
        if (inputStreamReader != null) {
          inputStreamReader.close();
        }
      } catch (IOException e) {
        // Ignore.
      }
    }

    return new RestResponse<>(secretManager.importSecrets(accountId, secretTexts));
  }

  @POST
  @Path("/add-secret")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveSecret(@QueryParam("accountId") final String accountId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.saveSecret(accountId, secretText.getName(), secretText.getValue(),
        secretText.getPath(), secretText.getUsageRestrictions()));
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
    return new RestResponse<>(secretManager.saveFile(accountId, name,
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit())));
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
    // HAR-9736: If the user doesn't make any change in the secret file update, null is expected for now.
    if (uploadedInputStream == null) {
      // fill in with an empty input stream
      uploadedInputStream = new ByteArrayInputStream(new byte[0]);
    }
    return new RestResponse<>(secretManager.updateFile(accountId, name, fileId,
        usageRestrictionsService.getUsageRestrictionsFromJson(usageRestrictionsString),
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
    return new RestResponse<>(secretManager.getSecretUsage(accountId, secretId));
  }
}
