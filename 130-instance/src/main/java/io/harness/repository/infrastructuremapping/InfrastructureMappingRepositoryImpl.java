package io.harness.repository.infrastructuremapping;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.repository.constants.Field;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InfrastructureMappingRepositoryImpl implements InfrastructureMappingRepository {
  private MongoTemplate mongoTemplate;

  @Override
  public InfrastructureMapping get(String accountId, String orgId, String projectId, String infrastructureMappingId) {
    Criteria criteria = Criteria.where(Field.ACCOUNT_ID)
                            .is(accountId)
                            .where(Field.ORG_ID)
                            .is(orgId)
                            .where(Field.PROJECT_ID)
                            .is(projectId)
                            .where(Field.ID)
                            .is(infrastructureMappingId);
    Query query = new Query().addCriteria(criteria);
    return mongoTemplate.findOne(query, InfrastructureMapping.class);
  }
}
