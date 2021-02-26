package io.harness.cvng.migration.list;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.migration.CNVGMigration;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class UpdateRiskIntToRiskEnum implements CNVGMigration {
  @Inject private HPersistence hPersistence;
  @Override
  public void migrate() {
    update(DeploymentTimeSeriesAnalysis.class);
  }

  private <T extends PersistentEntity> void update(Class<T> entityClazz) {
    for (Risk risk : Risk.values()) {
      UpdateOperations<T> updateOperations = hPersistence.createUpdateOperations(entityClazz);
      updateOperations.set("risk", risk);
      Query<T> query = hPersistence.createQuery(entityClazz).filter("risk", risk.getValue());
      log.info("updating risk {} for class {}", risk, entityClazz);
      UpdateResults updateResults = hPersistence.update(query, updateOperations);
      log.info("Updated count: {}", updateResults.getUpdatedCount());
    }
  }
}
