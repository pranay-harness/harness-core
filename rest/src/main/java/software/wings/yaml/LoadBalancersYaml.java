package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancersYaml extends BaseYaml {
  private List<String> ELB = new ArrayList<>();

  public List<String> getELB() {
    return ELB;
  }

  public void setELB(List<String> ELB) {
    this.ELB = ELB;
  }
}