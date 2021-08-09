package software.wings.helpers.ext.helm.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class SearchInfo {
  private String name;
  private String chartVersion;
  private String appVersion;
  private String description;
}
