package io.harness.ccm.cluster.entities;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class CEUserInfo {
  private String name;
  private String email;
  private List<String> clustersEnabled;
}
