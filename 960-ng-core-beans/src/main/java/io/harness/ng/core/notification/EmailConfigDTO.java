/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.notification;

import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("EMAIL")
public class EmailConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String groupEmail;

  @Builder
  public EmailConfigDTO(String groupEmail) {
    this.groupEmail = groupEmail;
    this.type = NotificationChannelType.EMAIL;
  }

  @Override
  public Optional<String> getSetting() {
    return Optional.ofNullable(groupEmail);
  }
}
