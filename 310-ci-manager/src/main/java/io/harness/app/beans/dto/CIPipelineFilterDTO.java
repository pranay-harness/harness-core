package io.harness.app.beans.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CIPipelineFilterDTO {
  @NotNull String accountIdentifier;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  String pipelineName;
  List<String> tags;
}
