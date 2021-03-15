package software.wings.resources.secretsmanagement;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;
import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import io.harness.beans.FeatureName;
import io.harness.eraro.ErrorCode;
import io.harness.exception.SecretManagementException;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagService;
import io.harness.rest.RestResponse;

import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.security.AwsSecretsManagerService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author marklu on 2019-05-07
 */
@Api("aws-secrets-manager")
@Path("/aws-secrets-manager")
@Produces("application/json")
@Scope(SETTING)
@AuthRule(permissionType = MANAGE_SECRET_MANAGERS)
public class AwsSecretsManagerResource {
  @Inject private AwsSecretsManagerService awsSecretsManagerService;
  @Inject private FeatureFlagService featureFlagService;

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveAwsSecretsManagerConfig(
      @QueryParam("accountId") final String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    checkFeatureFlag(accountId, secretsManagerConfig);
    return new RestResponse<>(awsSecretsManagerService.saveAwsSecretsManagerConfig(accountId, secretsManagerConfig));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteAwsSecretsManagerConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("configId") final String secretsManagerConfigId) {
    return new RestResponse<>(
        awsSecretsManagerService.deleteAwsSecretsManagerConfig(accountId, secretsManagerConfigId));
  }

  private void checkFeatureFlag(String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
    // check if feature is not enabled
    if (!featureFlagService.isEnabled(FeatureName.AWS_SM_ASSUME_IAM_ROLE, accountId)) {
      // none of the below values should be set if Feature is not enabled
      boolean usingAssumeRoleFeatures = secretsManagerConfig.isAssumeIamRoleOnDelegate()
          || secretsManagerConfig.isAssumeStsRoleOnDelegate() || isNotEmpty(secretsManagerConfig.getDelegateSelectors())
          || isNotEmpty(secretsManagerConfig.getRoleArn()) || isNotEmpty(secretsManagerConfig.getExternalName());
      if (usingAssumeRoleFeatures) {
        throw new SecretManagementException(ErrorCode.AWS_SECRETS_MANAGER_OPERATION_ERROR,
            "Feature flag " + FeatureName.AWS_SM_ASSUME_IAM_ROLE + " is not enabled for account:" + accountId,
            WingsException.USER_ADMIN);
      }
    }
  }
}
