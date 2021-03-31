package software.wings.service.intfc.sweepingoutput;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.deployment.InstanceDetails;

import software.wings.api.InstanceElement;
import software.wings.sm.StateExecutionInstance;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.mongodb.morphia.query.Query;

@OwnedBy(CDC)
public interface SweepingOutputService {
  SweepingOutputInstance save(@Valid SweepingOutputInstance sweepingOutputInstance);

  void ensure(@Valid SweepingOutputInstance sweepingOutputInstance);

  SweepingOutputInstance find(SweepingOutputInquiry inquiry);

  List<SweepingOutputInstance> findManyWithNamePrefix(
      SweepingOutputInquiry inquiry, SweepingOutputInstance.Scope scope);

  <T extends SweepingOutput> T findSweepingOutput(SweepingOutputInquiry inquiry);

  <T extends SweepingOutput> List<T> findSweepingOutputsWithNamePrefix(
      SweepingOutputInquiry inquiry, SweepingOutputInstance.Scope scope);

  void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId);

  Query<SweepingOutputInstance> prepareApprovalStateOutputsQuery(
      String appId, String fromPipelineExecutionId, String fromStateExecutionId);

  Query<SweepingOutputInstance> prepareEnvStateOutputsQuery(
      String appId, String fromPipelineExecutionId, String fromWorkflowExecutionId);

  void cleanForStateExecutionInstance(@NotNull StateExecutionInstance stateExecutionInstance);

  void deleteById(@NotNull String appId, @NotNull String uuid);

  List<InstanceDetails> fetchInstanceDetailsFromSweepingOutput(SweepingOutputInquiry inquiry, boolean newInstancesOnly);

  List<InstanceDetails> findInstanceDetailsForWorkflowExecution(String appId, String workflowExecutionId);

  List<InstanceElement> fetchInstanceElementsFromSweepingOutput(
      SweepingOutputInquiry inquiry, boolean newInstancesOnly);
}
