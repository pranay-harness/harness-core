package software.wings.beans.infrastructure.instance.info;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class PcfInstanceInfo extends InstanceInfo {
  private String id;
  private String organization;
  private String space;
  private String pcfApplicationName;
  private String pcfApplicationGuid;
  private String instanceIndex;
}
