package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Email")
public class EmailConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String groupEmail;
}