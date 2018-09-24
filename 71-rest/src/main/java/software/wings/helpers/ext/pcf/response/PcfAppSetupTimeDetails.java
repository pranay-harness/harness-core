package software.wings.helpers.ext.pcf.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class PcfAppSetupTimeDetails {
  private String applicationGuid;
  private String applicationName;
  private Integer initialInstanceCount;
  private List<String> urls;
}
