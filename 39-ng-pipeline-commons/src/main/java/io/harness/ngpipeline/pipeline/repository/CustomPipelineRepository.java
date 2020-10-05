package io.harness.ngpipeline.pipeline.repository;

import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface CustomPipelineRepository {
  Page<NgPipelineEntity> findAll(Criteria criteria, Pageable pageable);
  List<NgPipelineEntity> findAllWithCriteria(Criteria criteria);
  List<NgPipelineEntity> findAllWithCriteriaAndProjectOnFields(
      Criteria criteria, @NotNull List<String> includedFields, @NotNull List<String> excludedFields);
}
