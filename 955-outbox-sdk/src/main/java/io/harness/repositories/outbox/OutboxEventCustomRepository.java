package io.harness.repositories.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.OutboxEvent;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface OutboxEventCustomRepository {
  List<OutboxEvent> findAll(Criteria criteria, Pageable pageable);
}
