package software.wings.delegatetasks.helm;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class HelmChartDetails {
  private String name;
  private String version;
  @JsonProperty("app_version") private String appVersion;
  private String description;
}
