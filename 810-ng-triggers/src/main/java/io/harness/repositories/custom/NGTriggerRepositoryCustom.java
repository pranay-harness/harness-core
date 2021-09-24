package io.harness.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
public interface NGTriggerRepositoryCustom {
  Page<NGTriggerEntity> findAll(Criteria criteria, Pageable pageable);
  NGTriggerEntity update(Criteria criteria, NGTriggerEntity ngTriggerEntity);
  NGTriggerEntity updateValidationStatus(Criteria criteria, NGTriggerEntity ngTriggerEntity);
  UpdateResult delete(Criteria criteria);
}
