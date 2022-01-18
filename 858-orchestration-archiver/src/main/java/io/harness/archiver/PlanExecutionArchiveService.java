package io.harness.archiver;

import io.harness.archiver.beans.NodeExecutionArchive;
import io.harness.archiver.beans.PlanExecutionArchive;
import io.harness.plan.Plan;

import java.util.List;

public interface PlanExecutionArchiveService {
  PlanExecutionArchive save(PlanExecutionArchive planExecutionArchive);
}
