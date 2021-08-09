package software.wings.beans;

import io.harness.ccm.license.CeLicenseInfo;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CeLicenseUpdateInfo {
  @NotNull private CeLicenseInfo ceLicenseInfo;
  private Map<String, Map<String, Object>> requiredInfoToComply;
}
