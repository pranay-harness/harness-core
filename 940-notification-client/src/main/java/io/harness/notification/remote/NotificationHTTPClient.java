package io.harness.notification.remote;

import io.harness.Team;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.remote.dto.TemplateDTO;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;

public interface NotificationHTTPClient {
  @POST("channels/test")
  Call<ResponseDTO<Boolean>> testChannelSettings(@Body NotificationSettingDTO notificationSettingDTO);

  @Multipart
  @PUT("templates/insertOrUpdate")
  Call<ResponseDTO<TemplateDTO>> saveNotificationTemplate(@Part MultipartBody.Part file, @Part("team") Team team,
      @Part("identifier") String identifier, @Part("harnessManaged") Boolean harnessManaged);
}
