package software.wings.beans;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LicenseUpdateInfo {
  @NotNull private LicenseInfo licenseInfo;
  private Map<String, Map<String, Object>> requiredInfoToComply;
}
