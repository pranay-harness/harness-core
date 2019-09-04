package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.KmsConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.security.KmsService;
import software.wings.utils.AccountPermissionUtils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 10/10/17.
 */
@Api("kms")
@Path("/kms")
@Produces("application/json")
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class KmsResource {
  @Inject private KmsService kmsService;
  @Inject private AccountPermissionUtils accountPermissionUtils;

  @POST
  @Path("/save-global-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveGlobalKmsConfig(
      @QueryParam("accountId") final String accountId, KmsConfig kmsConfig) {
    RestResponse<String> response = accountPermissionUtils.checkIfHarnessUser("User not allowed to save global KMS");
    if (response == null) {
      response = new RestResponse<>(kmsService.saveGlobalKmsConfig(accountId, kmsConfig));
    }
    return response;
  }

  @POST
  @Path("/save-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveKmsConfig(@QueryParam("accountId") final String accountId, KmsConfig kmsConfig) {
    return new RestResponse<>(kmsService.saveKmsConfig(accountId, kmsConfig));
  }

  @GET
  @Path("/delete-kms")
  @Timed
  @ExceptionMetered
  // TODO: Delete this method once UI switched to use the new endpoint below.
  public RestResponse<Boolean> deleteKmsConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("kmsConfigId") final String kmsConfigId) {
    return new RestResponse<>(kmsService.deleteKmsConfig(accountId, kmsConfigId));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteKmsConfig2(
      @QueryParam("accountId") final String accountId, @QueryParam("kmsConfigId") final String kmsConfigId) {
    return new RestResponse<>(kmsService.deleteKmsConfig(accountId, kmsConfigId));
  }
}
