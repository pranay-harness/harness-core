package io.harness.ng.overview.dto;

import io.harness.timescaledb.tables.pojos.PipelineExecutionSummaryCd;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class TimeWiseExecutionSummary extends PipelineExecutionSummaryCd {
  private final long count;
  private final long epoch;

  public TimeWiseExecutionSummary(long epoch, String status, long count) {
    this.count = count;
    this.epoch = epoch;
    this.setStatus(status);
  }
}
