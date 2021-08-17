package software.wings.resources;
import static io.harness.annotations.dev.HarnessModule._955_ACCOUNT_MGMT;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;

import software.wings.beans.infrastructure.instance.InstanceSyncConfigService;
import software.wings.beans.infrastructure.instance.PerpetualTaskScheduleConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import javax.ws.rs.*;
import lombok.extern.slf4j.Slf4j;

@Api("account1")
@Path("/account1")
@Scope(PermissionAttribute.ResourceType.SETTING)
@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(_955_ACCOUNT_MGMT)
public class InstanceSyncConfigResource {
  private InstanceSyncConfigService instanceSyncConfigService;

  @Inject
  public InstanceSyncConfigResource(InstanceSyncConfigService instanceSyncConfigService) {
    this.instanceSyncConfigService = instanceSyncConfigService;
  }

  @GET
  @Path("/exportableCollections1")
  @Timed
  @ExceptionMetered
  public RestResponse<PerpetualTaskScheduleConfig> get(@QueryParam("accountId") String accountId,
      @QueryParam("perpetualTaskType") String perpetualTaskType, @QueryParam("timeInterval") int timeInterval) {
    return new RestResponse<>(
        instanceSyncConfigService.getByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType));
  }

  @POST
  @Path("/")
  @Timed
  @ExceptionMetered
  public RestResponse<PerpetualTaskScheduleConfig> save(@QueryParam("accountId") String accountId,
      @QueryParam("perpetualTaskType") String perpetualTaskType, @QueryParam("timeInterval") int timeInterval) {
    return new RestResponse<>(instanceSyncConfigService.save(accountId, perpetualTaskType, timeInterval));
  }
}
