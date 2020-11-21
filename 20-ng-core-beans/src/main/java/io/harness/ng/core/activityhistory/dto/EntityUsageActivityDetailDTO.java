package io.harness.ng.core.activityhistory.dto;

import io.harness.ng.core.EntityDetail;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@ApiModel("EntityUsageActivityDetail")
public class EntityUsageActivityDetailDTO implements ActivityDetail {
  @NotBlank EntityDetail referredByEntity;
  @NotEmpty String activityStatusMessage;
}
