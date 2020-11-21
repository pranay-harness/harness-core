package io.harness.notification.service;

import io.harness.NotificationRequest;
import io.harness.notification.NotificationChannelType;
import io.harness.notification.remote.dto.NotificationSettingDTO;
import io.harness.notification.service.api.*;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class ChannelServiceImpl implements ChannelService {
  Map<NotificationChannelType, ChannelService> implementationMapping = new HashMap<>();
  Map<NotificationRequest.ChannelCase, ChannelService> protoImplementationMapping = new HashMap<>();

  @Inject
  public ChannelServiceImpl(MailService mailService, SlackService slackService, PagerDutyService pagerDutyService,
      MSTeamsService msTeamsService) {
    implementationMapping.put(NotificationChannelType.EMAIL, mailService);
    implementationMapping.put(NotificationChannelType.PAGERDUTY, pagerDutyService);
    implementationMapping.put(NotificationChannelType.SLACK, slackService);
    implementationMapping.put(NotificationChannelType.MSTEAMS, msTeamsService);

    protoImplementationMapping.put(NotificationRequest.ChannelCase.EMAIL, mailService);
    protoImplementationMapping.put(NotificationRequest.ChannelCase.PAGERDUTY, pagerDutyService);
    protoImplementationMapping.put(NotificationRequest.ChannelCase.SLACK, slackService);
    protoImplementationMapping.put(NotificationRequest.ChannelCase.MSTEAM, msTeamsService);
  }

  @Override
  public boolean send(NotificationRequest notificationRequest) {
    return protoImplementationMapping.get(notificationRequest.getChannelCase()).send(notificationRequest);
  }

  @Override
  public boolean sendTestNotification(NotificationSettingDTO notificationSettingDTO) {
    return implementationMapping.get(notificationSettingDTO.getType()).sendTestNotification(notificationSettingDTO);
  }
}
