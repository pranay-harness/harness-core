package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

import java.util.List;

@JsonTypeName("PCF_PCF")
@Data
public class PcfInfraStructure implements CloudProviderInfrastructure {
  private String cloudProviderId;
  private String organization;
  private String space;
  private List<String> tempRouteMap;
  private List<String> routeMaps;
}
