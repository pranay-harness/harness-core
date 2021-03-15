package io.harness.pms.approval.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproverInputInfoDTO {
  @NotEmpty String name;
  String defaultValue;
}
