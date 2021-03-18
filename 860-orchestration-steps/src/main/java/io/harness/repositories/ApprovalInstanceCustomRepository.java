package io.harness.repositories;

import io.harness.steps.approval.step.entities.ApprovalInstance;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface ApprovalInstanceCustomRepository {
  ApprovalInstance update(Query query, Update update);
}
