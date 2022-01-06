/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.remote.resources;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.entities.NotificationSetting;
import io.harness.notification.remote.dto.AccountNotificationSettingDTO;
import io.harness.notification.remote.mappers.NotificationSettingMapper;
import io.harness.notification.service.api.NotificationSettingsService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@Api("settings")
@Path("settings")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class NotificationSettingsResource {
  private final NotificationSettingsService notificationSettingsService;

  @GET
  @ApiOperation(value = "Get NotificationSetting", nickname = "getNotificationSetting")
  public ResponseDTO<Optional<AccountNotificationSettingDTO>> getNotificationSetting(
      @QueryParam(ACCOUNT_KEY) @NotNull String accountId) {
    return ResponseDTO.newResponse(
        NotificationSettingMapper.toDTO(notificationSettingsService.getNotificationSetting(accountId).orElse(null)));
  }

  @PUT
  @ApiOperation(value = "Set sendNotificationViaDelegate", nickname = "postSendNotificationViaDelegate")
  public ResponseDTO<Optional<AccountNotificationSettingDTO>> putSendNotificationViaDelegate(
      @QueryParam(ACCOUNT_KEY) String accountId,
      @QueryParam("SendNotificationViaDelegate") boolean sendNotificationViaDelegate) {
    NotificationSetting notificationSetting =
        notificationSettingsService.setSendNotificationViaDelegate(accountId, sendNotificationViaDelegate);
    return ResponseDTO.newResponse(NotificationSettingMapper.toDTO(notificationSetting));
  }
}
