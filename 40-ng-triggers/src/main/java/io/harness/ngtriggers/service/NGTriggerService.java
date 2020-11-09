package io.harness.ngtriggers.service;

import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface NGTriggerService {
  NGTriggerEntity create(NGTriggerEntity ngTriggerEntity);

  Optional<NGTriggerEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String targetIdentifier, String identifier, boolean deleted);

  NGTriggerEntity update(NGTriggerEntity ngTriggerEntity);

  Page<NGTriggerEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String targetIdentifier,
      String identifier, Long version);
}
