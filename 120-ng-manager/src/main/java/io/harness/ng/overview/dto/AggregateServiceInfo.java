package io.harness.ng.overview.dto;

import io.harness.timescaledb.tables.pojos.ServiceInfraInfo;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AggregateServiceInfo extends ServiceInfraInfo {
  private long count;

  public AggregateServiceInfo(String orgIdentifier, String projectId, String serviceId, long count) {
    super(null, null, serviceId, null, null, null, null, null, null, null, null, null, null, orgIdentifier, projectId,
        null);
    this.count = count;
  }

  public AggregateServiceInfo(
      String orgIdentifier, String projectId, String serviceId, String serviceStatus, long count) {
    super(null, null, serviceId, null, null, null, null, null, null, serviceStatus, null, null, null, orgIdentifier,
        projectId, null);
    this.count = count;
  }
}
