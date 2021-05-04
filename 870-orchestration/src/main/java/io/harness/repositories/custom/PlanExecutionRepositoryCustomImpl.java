package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PlanExecutionRepositoryCustomImpl implements PlanExecutionRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<PlanExecution> findAll(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.find(query, PlanExecution.class);
  }
}
