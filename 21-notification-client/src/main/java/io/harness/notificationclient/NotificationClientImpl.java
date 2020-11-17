package io.harness.notificationclient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.NotificationRequest;
import io.harness.channeldetails.NotificationChannel;
import io.harness.messageclient.MessageClient;
import io.harness.notification.NotificationResult;
import io.harness.notification.NotificationResultWithoutStatus;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.remote.NotificationHTTPClient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static io.harness.remote.client.NGRestUtils.getResponse;

@Slf4j
@Getter
@Setter
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NotificationClientImpl implements NotificationClient {
  private MessageClient messageClient;
  private NotificationHTTPClient notificationHTTPClient;

  @Override
  public NotificationResult sendNotificationAsync(NotificationChannel notificationChannel) {
    NotificationRequest notificationRequest = notificationChannel.buildNotificationRequest();

    this.messageClient.send(notificationRequest, notificationChannel.getAccountId());
    return NotificationResultWithoutStatus.builder().notificationId(notificationRequest.getId()).build();
  }

  @Override
  public boolean testNotificationChannel(NotificationSettingDTO notificationSettingDTO) {
    return getResponse(notificationHTTPClient.testChannelSettings(notificationSettingDTO));
  }
}
