package io.harness.archiver;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.springdata.TransactionHelper.TransactionFunction;

import com.mongodb.client.result.DeleteResult;
import io.harness.archiver.beans.NodeExecutionArchive;
import io.harness.archiver.beans.PlanExecutionArchive;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.logging.AutoLogContext;
import io.harness.springdata.TransactionHelper;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class PlanExecutionArchiverImpl implements PlanExecutionArchiver {
  @Inject private TransactionHelper transactionHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PlanExecutionArchiveService planExecutionArchiveService;
  @Inject private NodeExecutionArchiveService nodeExecutionArchiveService;

  @Override
  public boolean archive(String planExecutionId) {
    ArchiveSession session = new ArchiveSession(planExecutionId, planExecutionService, nodeExecutionService,
        planExecutionArchiveService, nodeExecutionArchiveService);
    return transactionHelper.performTransaction(session);
  }

  @Override
  public boolean restore(String planExecutionId) {
    return false;
  }

  private static class ArchiveSession implements TransactionFunction<Boolean> {
    private final String planExecutionId;
    private final PlanExecutionService planExecutionService;
    private final NodeExecutionService nodeExecutionService;
    private final PlanExecutionArchiveService planExecutionArchiveService;
    private final NodeExecutionArchiveService nodeExecutionArchiveService;

    public ArchiveSession(String planExecutionId, PlanExecutionService planExecutionService,
        NodeExecutionService nodeExecutionService, PlanExecutionArchiveService planExecutionArchiveService,
        NodeExecutionArchiveService nodeExecutionArchiveService) {
      this.planExecutionId = planExecutionId;
      this.planExecutionService = planExecutionService;
      this.nodeExecutionService = nodeExecutionService;
      this.planExecutionArchiveService = planExecutionArchiveService;
      this.nodeExecutionArchiveService = nodeExecutionArchiveService;
    }

    @Override
    public Boolean execute() {
      try (AutoLogContext ignore = new ArchiverLogContext(planExecutionId, OVERRIDE_ERROR)) {
        PlanExecution planExecution = planExecutionService.get(planExecutionId);
        List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutions(planExecutionId);

        PlanExecutionArchive peArchive = PlanExecutionArchive.fromPlanExecution(planExecution);
        List<NodeExecutionArchive> neArchives =
            nodeExecutions.stream().map(NodeExecutionArchive::fromNodeExecution).collect(Collectors.toList());

        planExecutionArchiveService.save(peArchive);
        nodeExecutionArchiveService.saveAll(neArchives);

        DeleteResult neResult = nodeExecutionService.deleteAll(nodeExecutions.stream().map(NodeExecution::getUuid).collect(Collectors.toList()));
        if (!neResult.wasAcknowledged() || neResult.getDeletedCount() != nodeExecutions.size()){
          //TODO: create and throw a custom exception
          throw new InvalidRequestException("Delete of node executions were not successful");
        }
        DeleteResult peResult = planExecutionService.delete(planExecution);

        if (!peResult.wasAcknowledged() || peResult.getDeletedCount() != 1){
          //TODO: create and throw a custom exception
          throw new InvalidRequestException("Delete of plan executions was not successful");
        }
        return true;
      }
    }
  }
}
