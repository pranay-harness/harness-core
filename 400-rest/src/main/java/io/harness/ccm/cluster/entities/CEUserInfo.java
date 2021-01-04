package io.harness.ccm.cluster.entities;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CEUserInfo {
  private String name;
  private String email;
  private List<String> clustersEnabled;
}
