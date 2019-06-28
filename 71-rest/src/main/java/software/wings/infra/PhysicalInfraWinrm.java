package software.wings.infra;

import lombok.Data;
import software.wings.beans.infrastructure.Host;

import java.util.List;

@Data
public class PhysicalInfraWinrm implements PhysicalDataCenterInfra, CloudProviderInfrastructure {
  private String cloudProviderId;
  private List<String> hostNames;
  private List<Host> hosts;
  private String loadBalancerId;
  private String loadBalancerName;
  private String hostConnectionAttrs;
}
