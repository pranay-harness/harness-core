package io.harness.steps.approval.step.harness.beans;

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

  public static ApproverInputInfoDTO fromApproverInputInfo(ApproverInputInfo approverInput) {
    if (approverInput == null) {
      return null;
    }

    return ApproverInputInfoDTO.builder()
        .name(approverInput.getName())
        .defaultValue((String) approverInput.getDefaultValue().fetchFinalValue())
        .build();
  }
}
