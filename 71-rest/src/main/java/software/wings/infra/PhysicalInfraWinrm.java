package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@JsonTypeName("PHYSICAL_DATA_CENTER_WINRM")
@Data
public class PhysicalInfraWinrm implements PhysicalDataCenterInfra, CloudProviderInfrastructure {
  private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  private String loadBalancerName;
  private String hostConnectionAttrs;
}
