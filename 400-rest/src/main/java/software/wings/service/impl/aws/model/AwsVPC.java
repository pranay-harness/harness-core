package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsVPC implements Serializable {
  private String id;
  private String name;
}
