package software.wings.helpers.ext.helm.response;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

/**
 * Created by anubhaw on 4/2/18.
 */
@Data
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(HarnessTeam.DEL)
public class ReleaseInfo {
  private String name;
  private String revision;
  private String status;
  private String chart;
  private String namespace;
}
