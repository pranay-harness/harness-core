package software.wings.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.GcpKmsConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.utils.AccountPermissionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Api("gcp-secrets-manager")
@Path("/gcp-secrets-manager")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces("application/json")
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class GcpSecretsManagerResource {
  private GcpSecretsManagerService gcpSecretsManagerService;
  private AccountPermissionUtils accountPermissionUtils;
  private MainConfiguration configuration;

  @Inject
  GcpSecretsManagerResource(GcpSecretsManagerService gcpSecretsManagerService,
      AccountPermissionUtils accountPermissionUtils, MainConfiguration mainConfiguration) {
    this.gcpSecretsManagerService = gcpSecretsManagerService;
    this.accountPermissionUtils = accountPermissionUtils;
    this.configuration = mainConfiguration;
  }

  @PUT
  @Path("/global-kms")
  public RestResponse<String> saveGlobalKmsConfig(@QueryParam("accountId") final String accountId,
      @FormDataParam("name") String name, @FormDataParam("keyName") String keyName,
      @FormDataParam("keyRing") String keyRing, @FormDataParam("projectId") String projectId,
      @FormDataParam("region") String region, @FormDataParam("encryptionType") EncryptionType encryptionType,
      @FormDataParam("isDefault") boolean isDefault, @FormDataParam("credentials") InputStream uploadedInputStream)
      throws IOException {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      BoundedInputStream boundedInputStream =
          new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit());
      char[] credentials = IOUtils.toString(boundedInputStream, Charset.defaultCharset()).toCharArray();
      GcpKmsConfig gcpKmsConfig = new GcpKmsConfig(name, projectId, region, keyRing, keyName, credentials);
      gcpKmsConfig.setDefault(isDefault);
      gcpKmsConfig.setEncryptionType(encryptionType);

      RestResponse<String> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to save global KMS");
      if (response == null) {
        response = new RestResponse<>(gcpSecretsManagerService.saveGcpKmsConfig(GLOBAL_ACCOUNT_ID, gcpKmsConfig));
      }
      return response;
    }
  }

  @POST
  @Path("/{secretMangerId}")
  public RestResponse<String> updateGcpSecretsManagerConfig(@QueryParam("accountId") final String accountId,
      @PathParam("secretMangerId") final String secretMangerId, @FormDataParam("name") String name,
      @FormDataParam("keyName") String keyName, @FormDataParam("keyRing") String keyRing,
      @FormDataParam("projectId") String projectId, @FormDataParam("region") String region,
      @FormDataParam("encryptionType") EncryptionType encryptionType, @FormDataParam("isDefault") boolean isDefault,
      @FormDataParam("credentials") InputStream uploadedInputStream) throws IOException {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      BoundedInputStream boundedInputStream =
          new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit());
      char[] credentials = IOUtils.toString(boundedInputStream, Charset.defaultCharset()).toCharArray();
      GcpKmsConfig gcpKmsConfig = new GcpKmsConfig(name, projectId, region, keyRing, keyName, credentials);
      gcpKmsConfig.setUuid(secretMangerId);
      gcpKmsConfig.setDefault(isDefault);
      gcpKmsConfig.setEncryptionType(encryptionType);
      return new RestResponse<>(gcpSecretsManagerService.updateGcpKmsConfig(accountId, gcpKmsConfig));
    }
  }

  @POST
  public RestResponse<String> saveGcpSecretsManagerConfig(@QueryParam("accountId") final String accountId,
      @FormDataParam("name") String name, @FormDataParam("keyName") String keyName,
      @FormDataParam("keyRing") String keyRing, @FormDataParam("projectId") String projectId,
      @FormDataParam("region") String region, @FormDataParam("encryptionType") EncryptionType encryptionType,
      @FormDataParam("isDefault") boolean isDefault, @FormDataParam("credentials") InputStream uploadedInputStream)
      throws IOException {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      BoundedInputStream boundedInputStream =
          new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getEncryptedFileLimit());
      char[] credentials = IOUtils.toString(boundedInputStream, Charset.defaultCharset()).toCharArray();
      GcpKmsConfig gcpKmsConfig = new GcpKmsConfig(name, projectId, region, keyRing, keyName, credentials);
      gcpKmsConfig.setDefault(isDefault);
      gcpKmsConfig.setEncryptionType(encryptionType);
      return new RestResponse<>(gcpSecretsManagerService.saveGcpKmsConfig(accountId, gcpKmsConfig));
    }
  }

  @DELETE
  public RestResponse<Boolean> deleteGcpSecretsManagerConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("configId") final String secretsManagerConfigId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(gcpSecretsManagerService.deleteGcpKmsConfig(accountId, secretsManagerConfigId));
    }
  }
}
