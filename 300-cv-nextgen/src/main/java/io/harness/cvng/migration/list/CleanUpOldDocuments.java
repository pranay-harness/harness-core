/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine.AnalysisStateMachineKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
@Slf4j
public class CleanUpOldDocuments implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Override
  public void migrate() {
    Query<AnalysisOrchestrator> analysisOrchestratorQuery =
        hPersistence.createQuery(AnalysisOrchestrator.class)
            .filter(AnalysisOrchestratorKeys.status, AnalysisStatus.CREATED);
    List<AnalysisOrchestrator> analysisOrchestratorList = analysisOrchestratorQuery.asList();
    for (AnalysisOrchestrator analysisOrchestrator : analysisOrchestratorList) {
      if (analysisOrchestrator.getAnalysisStateMachineQueue().isEmpty()) {
        log.info("Going to delete invalid documents: {}", analysisOrchestrator);
        hPersistence.delete(
            hPersistence.createQuery(AnalysisOrchestrator.class)
                .filter(AnalysisOrchestratorKeys.verificationTaskId, analysisOrchestrator.getVerificationTaskId()));
        log.info("Deleted: {}", analysisOrchestrator);
      }
      if (analysisOrchestrator.getValidUntil() == null) {
        log.info("Going to delete document where valid until is null: {}", analysisOrchestrator);
        hPersistence.delete(
            hPersistence.createQuery(AnalysisOrchestrator.class)
                .filter(AnalysisOrchestratorKeys.verificationTaskId, analysisOrchestrator.getVerificationTaskId()));
        log.info("Deleted: {}", analysisOrchestrator);
      }
    }
    log.info("Deleting AnalysisStateMachine old documents");
    Query<AnalysisStateMachine> analysisStateMachineOldDocumentsQuery =
        hPersistence.createQuery(AnalysisStateMachine.class).field(AnalysisStateMachineKeys.validUntil).doesNotExist();
    log.info("Going to delete: {}", analysisStateMachineOldDocumentsQuery.count());
    boolean deleted = hPersistence.delete(analysisStateMachineOldDocumentsQuery);
    log.info("Deleted AnalysisStateMachine old documents {}", deleted);
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
