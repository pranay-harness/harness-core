package io.harness.ng.core.entities;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.notification.NotificationChannelType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@OwnedBy(HarnessTeam.PL)
public abstract class NotificationSettingConfig {
  @NotNull protected NotificationChannelType type;
}
