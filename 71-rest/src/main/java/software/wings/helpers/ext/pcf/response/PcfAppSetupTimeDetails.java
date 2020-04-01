package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PcfAppSetupTimeDetails {
  private String applicationGuid;
  private String applicationName;
  private Integer initialInstanceCount;
  private List<String> urls;
  private boolean activeApp;
}
