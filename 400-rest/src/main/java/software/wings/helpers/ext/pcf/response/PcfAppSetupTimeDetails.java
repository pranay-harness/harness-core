package software.wings.helpers.ext.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class PcfAppSetupTimeDetails {
  private String applicationGuid;
  private String applicationName;
  private Integer initialInstanceCount;
  private List<String> urls;
  private boolean activeApp;
}
