package io.harness.ng.core.entityReference.repositories;

import io.harness.ng.core.entityReference.entity.EntityReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface EntityReferenceCustomRepository {
  Page<EntityReference> findAll(Criteria criteria, Pageable pageable);
}
