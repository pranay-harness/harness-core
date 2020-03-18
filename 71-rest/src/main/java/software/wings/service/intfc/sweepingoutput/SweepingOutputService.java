package software.wings.service.intfc.sweepingoutput;

import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.deployment.InstanceDetails;
import software.wings.api.InstanceElement;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface SweepingOutputService {
  SweepingOutputInstance save(@Valid SweepingOutputInstance sweepingOutputInstance);

  void ensure(@Valid SweepingOutputInstance sweepingOutputInstance);

  SweepingOutputInstance find(SweepingOutputInquiry inquiry);

  <T extends SweepingOutput> T findSweepingOutput(SweepingOutputInquiry inquiry);

  void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId);

  void cleanForStateExecutionInstance(@NotNull StateExecutionInstance stateExecutionInstance);

  List<InstanceDetails> fetchInstanceDetailsFromSweepingOutput(SweepingOutputInquiry inquiry, boolean newInstancesOnly);

  List<InstanceElement> fetchInstanceElementsFromSweepingOutput(
      SweepingOutputInquiry inquiry, boolean newInstancesOnly);
}
