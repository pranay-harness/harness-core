package io.harness.ng.core.entityReference.repositories;

import com.google.inject.Inject;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.entityReference.entity.EntityReference;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

import java.util.List;

@HarnessRepo
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EntityReferenceCustomRepositoryImpl implements EntityReferenceCustomRepository {
  private final MongoTemplate mongoTemplate;

  public Page<EntityReference> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<EntityReference> connectors = mongoTemplate.find(query, EntityReference.class);
    return PageableExecutionUtils.getPage(
        connectors, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), EntityReference.class));
  }
}
