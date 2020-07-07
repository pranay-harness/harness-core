package software.wings.resources.secretsmanagement;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CustomSecretsManagerService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.GcpSecretsManagerService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secret-managers")
@Path("/ng/secret-managers")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
public class SecretManagerResourceNG {
  @Inject private SecretManager secretManager;
  @Inject private KmsService kmsService;
  @Inject private GcpSecretsManagerService gcpSecretsManagerService;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService awsSecretsManagerService;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private AzureSecretsManagerService azureSecretsManagerService;
  @Inject private CyberArkService cyberArkService;
  @Inject private CustomSecretsManagerService customSecretsManagerService;

  @POST
  public RestResponse<String> createOrUpdateSecretManager(
      @QueryParam("accountId") String accountId, SecretManagerConfig secretManagerConfig) {
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        return new RestResponse<>(vaultService.saveOrUpdateVaultConfig(accountId, (VaultConfig) secretManagerConfig));
      case CYBERARK:
        return new RestResponse<>(cyberArkService.saveConfig(accountId, (CyberArkConfig) secretManagerConfig));
      case AZURE_VAULT:
        return new RestResponse<>(azureSecretsManagerService.saveAzureSecretsManagerConfig(
            accountId, (AzureVaultConfig) secretManagerConfig));
      case CUSTOM:
        return new RestResponse<>(customSecretsManagerService.saveSecretsManager(
            accountId, (CustomSecretsManagerConfig) secretManagerConfig));
      case GCP_KMS:
        return new RestResponse<>(
            gcpSecretsManagerService.saveGcpKmsConfig(accountId, (GcpKmsConfig) secretManagerConfig));
      case KMS:
        return new RestResponse<>(kmsService.saveKmsConfig(accountId, (KmsConfig) secretManagerConfig));
      case AWS_SECRETS_MANAGER:
        return new RestResponse<>(awsSecretsManagerService.saveAwsSecretsManagerConfig(
            accountId, (AwsSecretsManagerConfig) secretManagerConfig));
      default:
        throw new InvalidRequestException(
            String.format("Encryption type: %s not supported", secretManagerConfig.getEncryptionType()), USER);
    }
  }

  @GET
  public RestResponse<List<SecretManagerConfig>> getSecretManagers(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(secretManager.listSecretManagers(accountId));
  }

  @GET
  @Path("{kmsId}")
  public RestResponse<SecretManagerConfig> getSecretManager(
      @QueryParam("accountId") String accountId, @PathParam("kmsId") String kmsId) {
    return new RestResponse<>(secretManager.getSecretManager(accountId, kmsId));
  }

  @DELETE
  @Path("/{kmsId}")
  public RestResponse<Boolean> deleteSecretManager(
      @QueryParam("accountId") String accountId, @PathParam("kmsId") String kmsId) {
    SecretManagerConfig secretManagerConfig = secretManager.getSecretManager(accountId, kmsId);
    switch (secretManagerConfig.getEncryptionType()) {
      case VAULT:
        return new RestResponse<>(vaultService.deleteVaultConfig(accountId, kmsId));
      case CYBERARK:
        return new RestResponse<>(cyberArkService.deleteConfig(accountId, kmsId));
      case AZURE_VAULT:
        return new RestResponse<>(azureSecretsManagerService.deleteConfig(accountId, kmsId));
      case CUSTOM:
        return new RestResponse<>(customSecretsManagerService.deleteSecretsManager(accountId, kmsId));
      case GCP_KMS:
        return new RestResponse<>(gcpSecretsManagerService.deleteGcpKmsConfig(accountId, kmsId));
      case KMS:
        return new RestResponse<>(kmsService.deleteKmsConfig(accountId, kmsId));
      case AWS_SECRETS_MANAGER:
        return new RestResponse<>(awsSecretsManagerService.deleteAwsSecretsManagerConfig(accountId, kmsId));
      default:
        throw new InvalidRequestException(
            String.format("Encryption type: %s not supported", secretManagerConfig.getEncryptionType()), USER);
    }
  }
}
