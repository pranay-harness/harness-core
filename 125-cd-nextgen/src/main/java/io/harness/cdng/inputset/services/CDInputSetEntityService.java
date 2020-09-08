package io.harness.cdng.inputset.services;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface CDInputSetEntityService {
  CDInputSetEntity create(CDInputSetEntity cdInputSetEntity);

  Optional<CDInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted);

  CDInputSetEntity update(CDInputSetEntity requestCDInputSet);

  CDInputSetEntity upsert(CDInputSetEntity requestCDInputSet);

  Page<CDInputSetEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier);
}
