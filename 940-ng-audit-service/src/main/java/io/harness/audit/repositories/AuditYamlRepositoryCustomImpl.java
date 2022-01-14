package io.harness.audit.repositories;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.entities.AuditEvent;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuditYamlRepositoryCustomImpl implements AuditYamlRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public void delete(Criteria criteria) {
    Query query = new Query(criteria);
    mongoTemplate.findAllAndRemove(query, AuditEvent.class);
  }
}
