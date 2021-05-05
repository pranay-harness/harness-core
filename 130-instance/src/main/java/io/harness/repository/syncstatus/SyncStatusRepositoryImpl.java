package io.harness.repository.syncstatus;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.SyncStatus;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class SyncStatusRepositoryImpl implements SyncStatusRepository {
  private MongoTemplate mongoTemplate;

  @Override
  public SyncStatus getSyncStatus(
      String orgId, String projectId, String serviceId, String envId, String infrastructureMappingId) {
    Criteria criteria = Criteria.where(SyncStatus.ORG_ID_KEY)
                            .is(orgId)
                            .where(SyncStatus.PROJECT_ID_KEY)
                            .is(projectId)
                            .where(SyncStatus.SERVICE_ID_KEY)
                            .is(serviceId)
                            .where(SyncStatus.ENV_ID_KEY)
                            .is(envId)
                            .where(SyncStatus.INFRA_MAPPING_ID_KEY)
                            .is(infrastructureMappingId);
    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.findOne(query, SyncStatus.class);
  }

  @Override
  public void deleteById(String syncStatusId) {
    Criteria criteria = Criteria.where(SyncStatus.ID_KEY).is(syncStatusId);
    Query query = new Query().addCriteria(criteria);
    mongoTemplate.remove(query, SyncStatus.class);
  }
}
