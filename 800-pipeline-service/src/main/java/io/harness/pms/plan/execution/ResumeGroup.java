package io.harness.pms.plan.execution;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.resume.ResumeStageInfo;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class ResumeGroup {
  private List<ResumeStageInfo> info;
}
