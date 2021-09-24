package software.wings.helpers.ext.helm.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DEL)
public class ReleaseInfo {
  private String name;
  private String revision;
  private String status;
  private String chart;
  private String namespace;
}
