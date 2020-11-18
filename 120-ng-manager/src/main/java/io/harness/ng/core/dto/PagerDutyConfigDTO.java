package io.harness.ng.core.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("PagerDuty")
public class PagerDutyConfigDTO extends NotificationSettingConfigDTO {
  @NotNull String pagerDutyId;
}
