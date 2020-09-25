package io.harness.cdng.inputset.services;

import io.harness.ngpipeline.BaseInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

public interface InputSetEntityService {
  BaseInputSetEntity create(BaseInputSetEntity baseInputSetEntity);

  Optional<BaseInputSetEntity> get(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, boolean deleted);

  BaseInputSetEntity update(BaseInputSetEntity baseInputSetEntity);

  Page<BaseInputSetEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      String inputSetIdentifier);

  String getTemplateFromPipeline(String pipelineYaml);
}
