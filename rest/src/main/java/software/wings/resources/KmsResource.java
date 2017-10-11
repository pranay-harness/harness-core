package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.Base;
import software.wings.beans.KmsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.UuidAware;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.security.KmsService;

import java.util.Collection;
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
@AuthRule(ResourceType.SETTING)
public class KmsResource {
  @Inject private KmsService kmsService;

  @POST
  @Path("/save-global-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveGlobalKmsConfig(
      @QueryParam("accountId") final String accountId, KmsConfig kmsConfig) {
    return new RestResponse<>(kmsService.saveKmsConfig(Base.GLOBAL_ACCOUNT_ID, kmsConfig));
  }

  @POST
  @Path("/save-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveKmsConfig(@QueryParam("accountId") final String accountId, KmsConfig kmsConfig) {
    return new RestResponse<>(kmsService.saveKmsConfig(accountId, kmsConfig));
  }

  @GET
  @Path("/delete-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteKmsConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("kmsConfigId") final String kmsConfigId) {
    return new RestResponse<>(kmsService.deleteKmsConfig(accountId, kmsConfigId));
  }

  @GET
  @Path("/list-values")
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<UuidAware>> listEncryptedValues(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(kmsService.listEncryptedValues(accountId));
  }

  @GET
  @Path("/transition-kms")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> transitionKms(@QueryParam("accountId") final String accountId,
      @QueryParam("fromKmsId") String fromKmsId, @QueryParam("fromKmsId") String toKmsId) {
    return new RestResponse<>(kmsService.transitionKms(accountId, fromKmsId, toKmsId));
  }
}
