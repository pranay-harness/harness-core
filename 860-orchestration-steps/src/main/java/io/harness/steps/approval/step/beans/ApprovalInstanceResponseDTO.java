package io.harness.steps.approval.step.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("ApprovalInstanceResponse")
@Schema(name = "ApprovalInstanceResponse", description = "This contains details of Approval Instance response")
public class ApprovalInstanceResponseDTO {
  @NotEmpty String id;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  long deadline;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  @Valid
  ApprovalInstanceDetailsDTO details;

  Long createdAt;
  Long lastModifiedAt;
}
