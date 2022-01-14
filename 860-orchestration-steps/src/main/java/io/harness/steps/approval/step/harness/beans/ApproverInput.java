package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApproverInput {
  @NotEmpty String name;
  @NotNull String value;

  public ApproverInputInfoDTO toApproverInputInfoDTO() {
    return ApproverInputInfoDTO.builder().name(name).defaultValue(value).build();
  }
}
