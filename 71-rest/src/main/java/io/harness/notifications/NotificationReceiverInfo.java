package io.harness.notifications;

import software.wings.beans.NotificationChannelType;
import software.wings.beans.notification.SlackNotificationSetting;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * This is kind of a union interface for NotificationGroup and UserGroup to help with migration between the two.
 * The interface can be deleted once {@link software.wings.beans.NotificationGroup} class is removed.
 */
public interface NotificationReceiverInfo {
  @NotNull Map<NotificationChannelType, List<String>> getAddressesByChannelType();

  @Nullable SlackNotificationSetting getSlackConfig();

  @NotNull List<String> getEmailAddresses();

  @Nullable String getPagerDutyIntegrationKey();

  @Nullable String getMicrosoftTeamsWebhookUrl();
}
