package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class LicenseUpdateInfo {
  @NotNull private LicenseInfo licenseInfo;
  private Map<String, Map<String, Object>> requiredInfoToComply;
}
