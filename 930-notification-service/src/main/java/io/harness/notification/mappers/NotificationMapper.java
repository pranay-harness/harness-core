package io.harness.notification.mappers;

import io.harness.notification.dtos.NotificationDTO;
import io.harness.notification.entities.Notification;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationMapper {
  public static Optional<NotificationDTO> toDTO(Notification notification) {
    if (notification == null) {
      return Optional.empty();
    }
    return Optional.of(NotificationDTO.builder()
                           .accountIdentifier(notification.getAccountIdentifier())
                           .channelType(notification.getChannel().getChannelType())
                           .id(notification.getId())
                           .retries(notification.getRetries())
                           .sent(notification.getSent())
                           .team(notification.getTeam())
                           .build());
  }
}
