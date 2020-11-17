package io.harness.channeldetails;

import com.google.inject.Inject;
import io.harness.NotificationRequest;
import io.harness.Team;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

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
