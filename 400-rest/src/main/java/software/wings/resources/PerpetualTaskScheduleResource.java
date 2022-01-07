/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.perpetualtask.PerpetualTaskScheduleService;
import io.harness.rest.RestResponse;

import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.HarnessUserGroupService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("perpetualTaskSchedule")
@Path("/perpetualTaskSchedule")
@Produces("application/json")
@Singleton
@Slf4j
@OwnedBy(PL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.beans.User")
@BreakDependencyOn("software.wings.security.UserThreadLocal")
public class PerpetualTaskScheduleResource {
  private PerpetualTaskScheduleService perpetualTaskScheduleService;
  private HarnessUserGroupService harnessUserGroupService;

  @Inject
  public PerpetualTaskScheduleResource(
      PerpetualTaskScheduleService perpetualTaskScheduleService, HarnessUserGroupService harnessUserGroupService) {
    this.perpetualTaskScheduleService = perpetualTaskScheduleService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PerpetualTaskScheduleConfig> getByAccountIdAndPerpetualTaskType(
      @QueryParam("accountId") String accountId, @QueryParam("perpetualTaskType") String perpetualTaskType) {
    if (!harnessUserGroupService.isHarnessSupportUser(UserThreadLocal.get().getUuid())) {
      throw new UnauthorizedException("You don't have the permissions to perform this action.", WingsException.USER);
    }
    return new RestResponse<>(
        perpetualTaskScheduleService.getByAccountIdAndPerpetualTaskType(accountId, perpetualTaskType));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<PerpetualTaskScheduleConfig> save(@QueryParam("accountId") String accountId,
      @QueryParam("perpetualTaskType") String perpetualTaskType,
      @QueryParam("timeIntervalInMillis") long timeIntervalInMillis) {
    if (!harnessUserGroupService.isHarnessSupportUser(UserThreadLocal.get().getUuid())) {
      throw new UnauthorizedException("You don't have the permissions to perform this action.", WingsException.USER);
    }
    return new RestResponse<>(perpetualTaskScheduleService.save(accountId, perpetualTaskType, timeIntervalInMillis));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> resetByAccountIdAndPerpetualTaskType(@QueryParam("accountId") String accountId,
      @QueryParam("perpetualTaskType") String perpetualTaskType,
      @QueryParam("timeIntervalInMillis") long timeIntervalInMillis) {
    if (!harnessUserGroupService.isHarnessSupportUser(UserThreadLocal.get().getUuid())) {
      throw new UnauthorizedException("You don't have the permissions to perform this action.", WingsException.USER);
    }
    return new RestResponse<>(perpetualTaskScheduleService.resetByAccountIdAndPerpetualTaskType(
        accountId, perpetualTaskType, timeIntervalInMillis));
  }
}
