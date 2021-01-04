package io.harness.notification.remote;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface NotificationHTTPClient {
  @POST("channels/test")
  Call<ResponseDTO<Boolean>> testChannelSettings(@Body NotificationSettingDTO notificationSettingDTO);
}
