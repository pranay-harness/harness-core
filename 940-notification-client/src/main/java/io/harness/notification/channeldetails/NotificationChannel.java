package io.harness.notification.channeldetails;

import io.harness.NotificationRequest;
import io.harness.Team;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public abstract class NotificationChannel {
  String accountId;
  List<String> userGroupIds;
  String templateId;
  Map<String, String> templateData;
  Team team;

  public abstract NotificationRequest buildNotificationRequest();
}
