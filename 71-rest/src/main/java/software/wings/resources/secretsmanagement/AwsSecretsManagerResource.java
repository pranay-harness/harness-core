package software.wings.resources.secretsmanagement;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.security.AwsSecretsManagerService;

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
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class AwsSecretsManagerResource {
  @Inject private AwsSecretsManagerService awsSecretsManagerService;

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveAwsSecretsManagerConfig(
      @QueryParam("accountId") final String accountId, AwsSecretsManagerConfig secretsManagerConfig) {
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
}
