package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class ServiceAccountCustomRepositoryImpl implements ServiceAccountCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Inject
  public ServiceAccountCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public long deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, ServiceAccountDBO.class).getDeletedCount();
  }
}
