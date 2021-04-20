package software.wings.api.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class PcfServiceData {
  private String name;
  private String id;
  private int previousCount;
  private int desiredCount;
  private boolean disableAutoscalarPerformed;
}
