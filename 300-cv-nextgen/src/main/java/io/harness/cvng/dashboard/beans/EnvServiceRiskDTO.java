package io.harness.cvng.dashboard.beans;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class EnvServiceRiskDTO {
  String orgIdentifier;
  String projectIdentifier;
  String envIdentifier;
  Integer risk;
  @Singular("addServiceRisk") Set<ServiceRisk> serviceRisks;

  @Data
  @Builder
  public static class ServiceRisk implements Comparable<ServiceRisk> {
    String serviceIdentifier;
    Integer risk;

    @Override
    public int compareTo(ServiceRisk o) {
      return Integer.compare(this.risk, o.risk);
    }
  }
}
