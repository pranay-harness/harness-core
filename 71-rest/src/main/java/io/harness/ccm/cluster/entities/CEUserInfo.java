package io.harness.ccm.cluster.entities;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CEUserInfo {
  private String name;
  private String email;
  private List<String> clustersEnabled;
}
