package software.wings.helpers.ext.pcf.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PcfAppSetupTimeDetails {
  private String applicationGuid;
  private String applicationName;
  private Integer initialInstanceCount;
  private List<String> urls;
  private boolean activeApp;
}
