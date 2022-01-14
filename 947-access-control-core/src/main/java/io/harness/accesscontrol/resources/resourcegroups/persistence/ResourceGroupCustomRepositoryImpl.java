package io.harness.accesscontrol.resources.resourcegroups.persistence;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@HarnessRepo
public class ResourceGroupCustomRepositoryImpl implements ResourceGroupCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public ResourceGroupCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<ResourceGroupDBO> findAllWithCriteria(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, ResourceGroupDBO.class);
  }

  @Override
  public Optional<ResourceGroupDBO> find(Criteria criteria) {
    Query query = new Query(criteria);
    return Optional.ofNullable(mongoTemplate.findOne(query, ResourceGroupDBO.class));
  }

  @Override
  public boolean deleteMulti(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, ResourceGroupDBO.class).wasAcknowledged();
  }
}
