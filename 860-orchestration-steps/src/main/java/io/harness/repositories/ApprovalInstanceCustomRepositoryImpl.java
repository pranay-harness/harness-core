package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.steps.approval.step.entities.ApprovalInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@HarnessRepo
public class ApprovalInstanceCustomRepositoryImpl implements ApprovalInstanceCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Autowired
  public ApprovalInstanceCustomRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public ApprovalInstance update(Query query, Update update) {
    return mongoTemplate.findAndModify(
        query, update, new FindAndModifyOptions().returnNew(true), ApprovalInstance.class);
  }
}
