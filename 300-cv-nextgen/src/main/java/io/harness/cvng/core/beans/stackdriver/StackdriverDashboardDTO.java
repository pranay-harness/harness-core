package io.harness.cvng.core.beans.stackdriver;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StackdriverDashboardDTO {
  String name;
  String path;
}
