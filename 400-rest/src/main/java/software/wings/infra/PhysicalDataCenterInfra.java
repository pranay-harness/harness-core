package software.wings.infra;

import software.wings.beans.infrastructure.Host;

import java.util.List;

public interface PhysicalDataCenterInfra {
  List<String> getHostNames();
  List<Host> getHosts();
  String getLoadBalancerId();
  String getLoadBalancerName();
}
