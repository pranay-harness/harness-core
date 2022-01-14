package io.harness.delegate.beans.pcf;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
public class CfAppSetupTimeDetails {
  private String applicationGuid;
  private String applicationName;
  private String oldName;
  private Integer initialInstanceCount;
  private List<String> urls;
  private boolean activeApp;
}
