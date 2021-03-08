package io.harness.notification.service.api;

import io.harness.Team;

import java.util.List;
import java.util.Map;

public interface MailService extends ChannelService {
  boolean send(
      List<String> emailIds, String templateId, Map<String, String> templateData, String notificationId, Team team);
  boolean send(List<String> emailIds, String templateId, Map<String, String> templateData, String notificationId);
}
