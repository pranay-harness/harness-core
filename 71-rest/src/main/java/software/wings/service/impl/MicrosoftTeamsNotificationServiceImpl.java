package software.wings.service.impl;

import static org.apache.http.entity.mime.MIME.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import software.wings.service.intfc.MicrosoftTeamsNotificationService;

@Slf4j
public class MicrosoftTeamsNotificationServiceImpl implements MicrosoftTeamsNotificationService {
  private final OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
  private static final MediaType APPLICATION_JSON = MediaType.parse(APPLICATION_JSON_UTF8_VALUE);

  @Override
  public int sendMessage(String message, String webhookUrl) {
    try {
      RequestBody body = RequestBody.create(APPLICATION_JSON, message);
      Request request =
          new Request.Builder().url(webhookUrl).post(body).addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).build();
      Response response = okHttpClient.newCall(request).execute();
      if (!response.isSuccessful()) {
        String bodyString = (null != response.body()) ? response.body().string() : "null";
        logger.error("Response not Successful. Response body: {}", bodyString);
      }
      return response.code();
    } catch (Exception e) {
      logger.error("Exception occurred at sendMessage(). Returning 400", e);
      return 400;
    }
  }
}
