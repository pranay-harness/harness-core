package io.harness.notification.service.api;

import io.harness.Team;

import java.util.List;
import java.util.Map;

public interface PagerDutyService extends ChannelService {
  boolean send(List<String> pagerDutyKeys, String templateId, Map<String, String> templateData, String notificationId);

  boolean send(List<String> pagerDutyKeys, String templateId, Map<String, String> templateData, String notificationId,
      Team team);
}
