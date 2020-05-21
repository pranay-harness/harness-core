package software.wings.resources.secretsmanagement;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.FeatureName.CUSTOM_SECRETS_MANAGER;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import com.google.inject.Inject;

import io.harness.exception.HintException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.CustomSecretsManagerService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("custom-secrets-managers")
@Path("/custom-secrets-managers")
@Produces("application/json")
@Scope(SETTING)
@AuthRule(permissionType = ACCOUNT_MANAGEMENT)
@Slf4j
public class CustomSecretsManagerResource {
  private CustomSecretsManagerService customSecretsManagerService;
  private FeatureFlagService featureFlagService;

  @Inject
  CustomSecretsManagerResource(
      CustomSecretsManagerService customSecretsManagerService, FeatureFlagService featureFlagService) {
    this.customSecretsManagerService = customSecretsManagerService;
    this.featureFlagService = featureFlagService;
  }

  @GET
  @Path("{configId}")
  public RestResponse<CustomSecretsManagerConfig> getCustomSecretsManagerConfig(
      @QueryParam("accountId") String accountId, @PathParam("configId") String configId) {
    checkIfFeatureAvailable(accountId);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(customSecretsManagerService.getSecretsManager(accountId, configId));
    }
  }

  @POST
  @Path("validate")
  public RestResponse<Boolean> validateCustomSecretsManagerConfig(
      @QueryParam("accountId") String accountId, CustomSecretsManagerConfig customSecretsManagerConfig) {
    checkIfFeatureAvailable(accountId);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          customSecretsManagerService.validateSecretsManager(accountId, customSecretsManagerConfig));
    }
  }

  @PUT
  public RestResponse<String> saveCustomSecretsManagerConfig(
      @QueryParam("accountId") String accountId, CustomSecretsManagerConfig customSecretsManagerConfig) {
    checkIfFeatureAvailable(accountId);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(customSecretsManagerService.saveSecretsManager(accountId, customSecretsManagerConfig));
    }
  }

  @POST
  @Path("{configId}")
  public RestResponse<String> updateCustomSecretsManagerConfig(@QueryParam("accountId") String accountId,
      @PathParam("configId") String configId, CustomSecretsManagerConfig customSecretsManagerConfig) {
    checkIfFeatureAvailable(accountId);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          customSecretsManagerService.updateSecretsManager(accountId, customSecretsManagerConfig));
    }
  }

  @DELETE
  @Path("{configId}")
  public RestResponse<Boolean> deleteCustomSecretsManagerConfig(
      @QueryParam("accountId") String accountId, @QueryParam("configId") String configId) {
    checkIfFeatureAvailable(accountId);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(customSecretsManagerService.deleteSecretsManager(accountId, configId));
    }
  }

  private void checkIfFeatureAvailable(String accountId) {
    if (!featureFlagService.isEnabled(CUSTOM_SECRETS_MANAGER, accountId)) {
      throw new HintException("This feature is not available for your account");
    }
  }
}
