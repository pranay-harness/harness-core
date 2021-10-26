package io.harness.ng.cdOverview.dto;

import io.harness.timescaledb.tables.pojos.PipelineExecutionSummaryCd;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class AggregateProjectInfo extends PipelineExecutionSummaryCd {
  private final long count;

  public AggregateProjectInfo(String orgIdentifier, String projectId, long count) {
    this.count = count;
    this.setOrgidentifier(orgIdentifier);
    this.setProjectidentifier(projectId);
  }

  public AggregateProjectInfo(String orgIdentifier, String projectId, String status, long count) {
    this.count = count;
    this.setOrgidentifier(orgIdentifier);
    this.setProjectidentifier(projectId);
    this.setStatus(status);
  }
}
