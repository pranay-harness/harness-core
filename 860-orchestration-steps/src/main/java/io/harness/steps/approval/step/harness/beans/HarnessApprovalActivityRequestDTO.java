package io.harness.steps.approval.step.harness.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("HarnessApprovalActivityRequest")
public class HarnessApprovalActivityRequestDTO {
  @NotNull HarnessApprovalAction action;
  List<ApproverInput> approverInputs;
  String comments;
}
