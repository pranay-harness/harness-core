package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditEvent;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditRepositoryCustomImpl implements AuditRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<AuditEvent> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<AuditEvent> auditEvents = mongoTemplate.find(query, AuditEvent.class);
    return PageableExecutionUtils.getPage(
        auditEvents, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AuditEvent.class));
  }

  @Override
  public AuditEvent get(Criteria criteria) {
    Query query = new Query(criteria);
    return mongoTemplate.findOne(query, AuditEvent.class);
  }
}
