package io.harness.notification;

import static io.harness.NotificationRequest.ChannelCase;

import io.harness.NotificationRequest;
import io.harness.notification.service.api.*;

import com.google.inject.Inject;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationRequestProcessor {
  private final Map<ChannelCase, ChannelService> channelToServiceMap;

  @Inject
  public NotificationRequestProcessor(MailService mailService, SlackService slackService,
      PagerDutyService pagerdutyService, MSTeamsService microsoftTeamsService) {
    channelToServiceMap = new EnumMap<>(ChannelCase.class);
    channelToServiceMap.put(ChannelCase.EMAIL, mailService);
    channelToServiceMap.put(ChannelCase.SLACK, slackService);
    channelToServiceMap.put(ChannelCase.PAGERDUTY, pagerdutyService);
    channelToServiceMap.put(ChannelCase.MSTEAM, microsoftTeamsService);
  }

  public boolean process(NotificationRequest notificationRequest) {
    ChannelCase channel = notificationRequest.getChannelCase();
    if (channelToServiceMap.containsKey(channel)) {
      return channelToServiceMap.get(channel).send(notificationRequest);
    }
    log.error("No channel service registered for handling notification request of kind {}", channel);
    return true;
  }
}
