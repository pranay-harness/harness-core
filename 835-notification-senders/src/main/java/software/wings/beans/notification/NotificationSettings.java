/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.notification;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;

import software.wings.beans.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
@OwnedBy(PL)
public class NotificationSettings {
  private boolean useIndividualEmails;
  private boolean sendMailToNewMembers;
  @NotNull private List<String> emailAddresses;
  @NotNull private SlackNotificationSetting slackConfig;
  private String pagerDutyIntegrationKey;
  private String microsoftTeamsWebhookUrl;

  @Nonnull
  @JsonIgnore
  public Map<NotificationChannelType, List<String>> getAddressesByChannelType() {
    return Collections.emptyMap();
  }

  @NotNull
  public SlackNotificationSetting getSlackConfig() {
    return slackConfig;
  }

  public List<String> getEmailAddresses() {
    return CollectionUtils.emptyIfNull(emailAddresses);
  }
}
