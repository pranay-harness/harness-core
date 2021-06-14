package software.wings.resources.secretsmanagement;

import static io.harness.beans.FeatureName.VAULT_AGENT;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.eraro.ErrorCode;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;

import software.wings.beans.VaultConfig;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.security.VaultService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rsingh on 10/10/17.
 */
@Api("vault")
@Path("/vault")
@Produces("application/json")
@Scope(SETTING)
@AuthRule(permissionType = MANAGE_SECRET_MANAGERS)
@Slf4j
public class VaultResource {
  @Inject private VaultService vaultService;
  @Inject private FeatureFlagService featureFlagService;

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveOrUpdateVaultConfig(
      @QueryParam("accountId") final String accountId, VaultConfig vaultConfig) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      checkIfFeatureIsEnabled(accountId, vaultConfig);
      log.info("Adding a vault config");
      return new RestResponse<>(vaultService.saveOrUpdateVaultConfig(accountId, vaultConfig, true));
    }
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteVaultConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("vaultConfigId") final String vaultConfigId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      log.info("Deleting a vault config");
      return new RestResponse<>(vaultService.deleteVaultConfig(accountId, vaultConfigId));
    }
  }

  @POST
  @Path("list-engines")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SecretEngineSummary>> listSecretEngines(
      @QueryParam("accountId") final String accountId, VaultConfig vaultConfig) {
    checkIfFeatureIsEnabled(accountId, vaultConfig);
    vaultConfig.setAccountId(accountId);
    return new RestResponse<>(vaultService.listSecretEngines(vaultConfig));
  }

  private void checkIfFeatureIsEnabled(@QueryParam("accountId") String accountId, VaultConfig vaultConfig) {
    if (!featureFlagService.isEnabled(VAULT_AGENT, accountId)) {
      boolean usingVaultAgentFeatures = vaultConfig.isUseVaultAgent() || isNotEmpty(vaultConfig.getSinkPath())
          || isNotEmpty(vaultConfig.getDelegateSelectors());
      if (usingVaultAgentFeatures) {
        throw new SecretManagementException(ErrorCode.VAULT_OPERATION_ERROR,
            "Feature flag " + VAULT_AGENT + " is not enabled for account:" + accountId, WingsException.USER_ADMIN);
      }
    }
  }
}