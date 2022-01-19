package io.harness.archiver;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import io.harness.archiver.nodeexecution.NodeExecutionArchive;
import io.harness.archiver.nodeexecution.NodeExecutionArchiveService;
import io.harness.archiver.planexecution.PlanExecutionArchive;
import io.harness.archiver.planexecution.PlanExecutionArchiveService;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.logging.AutoLogContext;
import io.harness.springdata.TransactionHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

import static io.harness.springdata.TransactionHelper.TransactionFunction;

@Slf4j
public class PlanExecutionArchiverImpl implements PlanExecutionArchiver {
  @Inject private TransactionHelper transactionHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private PlanExecutionArchiveService planExecutionArchiveService;
  @Inject private NodeExecutionArchiveService nodeExecutionArchiveService;

  @Override
  public boolean archive(String planExecutionId) {
    PlanExecution planExecution = null;
    try {
      planExecution = planExecutionService.get(planExecutionId);
    } catch (InvalidRequestException ex) {
      log.info("Plan execution deleted archived already {}", planExecutionId);
      return true;
    }

    List<NodeExecution> nodeExecutions = nodeExecutionService.fetchNodeExecutions(planExecutionId);
    ArchiveSession session = new ArchiveSession(planExecution, nodeExecutions, planExecutionService,
        nodeExecutionService, planExecutionArchiveService, nodeExecutionArchiveService);
    return transactionHelper.performTransaction(session);
  }

  @Override
  public boolean restore(String planExecutionId) {
    return false;
  }

  private static class ArchiveSession implements TransactionFunction<Boolean> {
    private final PlanExecution planExecution;
    private final List<NodeExecution> nodeExecutions;
    private final PlanExecutionService planExecutionService;
    private final NodeExecutionService nodeExecutionService;
    private final PlanExecutionArchiveService planExecutionArchiveService;
    private final NodeExecutionArchiveService nodeExecutionArchiveService;

    public ArchiveSession(PlanExecution planExecution, List<NodeExecution> nodeExecutions,
        PlanExecutionService planExecutionService, NodeExecutionService nodeExecutionService,
        PlanExecutionArchiveService planExecutionArchiveService,
        NodeExecutionArchiveService nodeExecutionArchiveService) {
      this.planExecution = planExecution;
      this.nodeExecutions = nodeExecutions;
      this.planExecutionService = planExecutionService;
      this.nodeExecutionService = nodeExecutionService;
      this.planExecutionArchiveService = planExecutionArchiveService;
      this.nodeExecutionArchiveService = nodeExecutionArchiveService;
    }

    @Override
    public Boolean execute() {
      try (AutoLogContext ignore = planExecution.autoLogContext()) {
        PlanExecutionArchive peArchive = PlanExecutionArchive.fromPlanExecution(planExecution);
        List<NodeExecutionArchive> neArchives =
            nodeExecutions.stream().map(NodeExecutionArchive::fromNodeExecution).collect(Collectors.toList());

        planExecutionArchiveService.save(peArchive);
        nodeExecutionArchiveService.saveAll(neArchives);

        DeleteResult neResult = nodeExecutionService.deleteAll(
            nodeExecutions.stream().map(NodeExecution::getUuid).collect(Collectors.toList()));
        if (!neResult.wasAcknowledged() || neResult.getDeletedCount() != nodeExecutions.size()) {
          // TODO: create and throw a custom exception
          throw new InvalidRequestException("Delete of node executions were not successful");
        }
        DeleteResult peResult = planExecutionService.delete(planExecution);

        if (!peResult.wasAcknowledged() || peResult.getDeletedCount() != 1) {
          // TODO: create and throw a custom exception
          throw new InvalidRequestException("Delete of plan executions was not successful");
        }
        return true;
      }
    }
  }
}
