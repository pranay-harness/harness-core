package io.harness.ngpipeline.pipeline.repository.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface NgPipelineRepositoryCustom {
  Page<NgPipelineEntity> findAll(Criteria criteria, Pageable pageable);
  UpdateResult update(Criteria criteria, NgPipelineEntity ngPipelineEntity);
  UpdateResult delete(Criteria criteria);
  List<NgPipelineEntity> findAllWithCriteriaAndProjectOnFields(
      Criteria criteria, @NotNull List<String> includedFields, @NotNull List<String> excludedFields);
}
