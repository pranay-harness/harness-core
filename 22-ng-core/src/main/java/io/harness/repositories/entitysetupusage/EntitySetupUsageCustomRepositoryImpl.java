package io.harness.repositories.entitysetupusage;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entitysetupusage.entity.EntitySetupUsage;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@HarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EntitySetupUsageCustomRepositoryImpl implements EntitySetupUsageCustomRepository {
  private final MongoTemplate mongoTemplate;

  public Page<EntitySetupUsage> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<EntitySetupUsage> connectors = mongoTemplate.find(query, EntitySetupUsage.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), EntitySetupUsage.class));
  }
}
