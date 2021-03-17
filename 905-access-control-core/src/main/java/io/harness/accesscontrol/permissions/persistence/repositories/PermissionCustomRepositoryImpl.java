package io.harness.accesscontrol.permissions.persistence.repositories;

import io.harness.accesscontrol.permissions.persistence.PermissionDBO;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class PermissionCustomRepositoryImpl implements PermissionCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public PermissionCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<PermissionDBO> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, PermissionDBO.class);
  }
}
