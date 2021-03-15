package io.harness.pms.approval.beans;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApproversDTO {
  List<String> userGroups;
  List<String> users;
  int minimumCount;
  boolean disallowPipelineExecutor;
}
