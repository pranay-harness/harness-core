package io.harness.repositories;

import io.harness.outbox.OutboxEvent;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class OutboxEventCustomRepositoryImpl implements OutboxEventCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<OutboxEvent> findAll(Pageable pageable) {
    Query query = new Query().with(pageable);
    List<OutboxEvent> outboxEvents = mongoTemplate.find(query, OutboxEvent.class);
    return PageableExecutionUtils.getPage(
        outboxEvents, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), OutboxEvent.class));
  }
}
