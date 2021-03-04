package io.harness.repositories;

import io.harness.outbox.Outbox;

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

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OutboxCustomRepositoryImpl implements OutboxCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Outbox> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Outbox> outboxes = mongoTemplate.find(query, Outbox.class);
    return PageableExecutionUtils.getPage(
        outboxes, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Outbox.class));
  }
}
