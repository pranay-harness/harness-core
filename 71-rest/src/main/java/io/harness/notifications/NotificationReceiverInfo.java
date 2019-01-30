package io.harness.notifications;

import software.wings.beans.NotificationChannelType;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public interface NotificationReceiverInfo {
  @NotNull Map<NotificationChannelType, List<String>> getAddressesByChannelType();
}
