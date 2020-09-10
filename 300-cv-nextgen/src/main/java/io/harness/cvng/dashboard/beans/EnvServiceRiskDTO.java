package io.harness.cvng.dashboard.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Set;

@Data
@Builder
public class EnvServiceRiskDTO {
  String orgIdentifier;
  String projectIdentifier;
  String envIdentifier;

  @Singular("addServiceRisk") Set<ServiceRisk> serviceRisks;

  @Data
  @Builder
  public static class ServiceRisk {
    String serviceIdentifier;
    Integer risk;
  }
}
