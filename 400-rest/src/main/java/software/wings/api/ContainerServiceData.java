package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

/**
 * Created by bzane on 9/11/17.
 */
@Data
@Builder
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class ContainerServiceData {
  private String name;
  private String image;
  // Use this if name can not be unique, like in case of ECS daemonSet
  private String uniqueIdentifier;
  private int previousCount;
  private int desiredCount;
  private int previousTraffic;
  private int desiredTraffic;
}
