package io.harness.app.beans.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CIBuildFilterDTO {
  @NotNull String accountIdentifier;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  String userIdentifier;
  String pipelineName;
  String branch;
  List<String> tags;
}
