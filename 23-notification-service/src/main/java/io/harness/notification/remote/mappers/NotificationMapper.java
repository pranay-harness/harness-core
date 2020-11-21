package io.harness.notification.remote.mappers;

import static io.harness.NotificationRequest.*;

import io.harness.NotificationRequest;
import io.harness.notification.entities.*;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class NotificationMapper {
  public static Notification toNotification(NotificationRequest notificationRequest) {
    try {
      return Notification.builder()
          .id(notificationRequest.getId())
          .accountIdentifier(notificationRequest.getAccountId())
          .team(notificationRequest.getTeam())
          .channel(channelDetailsProtoToMongo(notificationRequest))
          .build();
    } catch (Exception e) {
      log.error(
          "Error converting notification request to notification for persistence, check consistency of mongo entity and protobuf schema, {}",
          notificationRequest, e);
      return null;
    }
  }

  private static Channel channelDetailsProtoToMongo(NotificationRequest notificationRequest) {
    switch (notificationRequest.getChannelCase()) {
      case EMAIL:
        return EmailChannel.toEmailEntity(notificationRequest.getEmail());
      case SLACK:
        return SlackChannel.toSlackEntity(notificationRequest.getSlack());
      case PAGERDUTY:
        return PagerDutyChannel.toPagerDutyEntity(notificationRequest.getPagerDuty());
      case MSTEAM:
        return MicrosoftTeamsChannel.toMicrosoftTeamsEntity(notificationRequest.getMsTeam());
      default:
        log.error("Channel type of the notification request unidentified {}", notificationRequest.getChannelCase());
    }
    return null;
  }

  public static NotificationRequest toNotificationRequest(Notification notification) {
    try {
      NotificationRequest.Builder builder = newBuilder()
                                                .setId(notification.getId())
                                                .setAccountId(notification.getAccountIdentifier())
                                                .setTeam(notification.getTeam());
      setChannel(builder, notification);
      return builder.build();
    } catch (Exception e) {
      log.error(
          "Error converting notification to notificatino request, check consistency of mongo entity and protobuf schema, {}",
          notification, e);
      return null;
    }
  }

  private static void setChannel(NotificationRequest.Builder builder, Notification notification) {
    Object channelDetails = notification.getChannel().toObjectofProtoSchema();
    if (channelDetails instanceof Email) {
      builder.setEmail((Email) channelDetails);
    } else if (channelDetails instanceof Slack) {
      builder.setSlack((Slack) channelDetails);
    } else if (channelDetails instanceof PagerDuty) {
      builder.setPagerDuty((PagerDuty) channelDetails);
    } else if (channelDetails instanceof MSTeam) {
      builder.setMsTeam((MSTeam) channelDetails);
    }
  }
}
