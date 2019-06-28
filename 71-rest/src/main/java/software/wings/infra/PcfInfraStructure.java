package software.wings.infra;

import lombok.Data;

import java.util.List;

@Data
public class PcfInfraStructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String organization;
  private String space;
  private List<String> tempRouteMap;
  private List<String> routeMaps;
}
