package io.harness.repositories;

import io.harness.steps.approval.step.entities.ApprovalInstance;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface ApprovalInstanceCustomRepository {
  ApprovalInstance updateFirst(Query query, Update update);
  UpdateResult updateMulti(Query query, Update update);
}
