package software.wings.helpers.ext.cloudformation.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StackSummaryInfo {
  private String stackId;
  private String stackName;
  private String stackStatus;
  private String stackStatusReason;
}