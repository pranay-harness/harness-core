/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class EnvServiceRiskDTO {
  String orgIdentifier;
  String projectIdentifier;
  String envName;
  String envIdentifier;
  Integer risk;
  @Singular("addServiceRisk") List<ServiceRisk> serviceRisks;

  @Data
  @Builder
  public static class ServiceRisk implements Comparable<ServiceRisk> {
    String serviceName;
    String serviceIdentifier;
    Integer risk;

    @Override
    public int compareTo(ServiceRisk o) {
      return Integer.compare(this.risk, o.risk);
    }
  }
}
