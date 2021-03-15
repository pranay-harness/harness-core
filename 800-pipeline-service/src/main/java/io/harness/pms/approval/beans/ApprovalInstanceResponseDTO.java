package io.harness.pms.approval.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ApprovalInstanceResponse")
public class ApprovalInstanceResponseDTO {
  @NotEmpty String id;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  String approvalMessage;
  boolean includePipelineExecutionHistory;
  long deadline;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  ApprovalInstanceDetailsDTO details;

  Long createdAt;
  Long lastModifiedAt;
}
