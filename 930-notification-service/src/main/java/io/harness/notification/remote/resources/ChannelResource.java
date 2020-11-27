package io.harness.notification.remote.resources;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.ChannelService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Api("channels")
@Path("channels")
@Produces({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class ChannelResource {
  private final ChannelService channelService;

  @POST
  @Path("/test")
  @ApiOperation(value = "Test notification setting", nickname = "testNotificationSetting")
  public ResponseDTO<Boolean> testNotificationSetting(@NotNull @Valid NotificationSettingDTO notificationSettingDTO) {
    log.info("Received test notification request for {} - notificationId: {}", notificationSettingDTO.getType(),
        notificationSettingDTO.getNotificationId());
    boolean result = channelService.sendTestNotification(notificationSettingDTO);
    return ResponseDTO.newResponse(result);
  }
}
