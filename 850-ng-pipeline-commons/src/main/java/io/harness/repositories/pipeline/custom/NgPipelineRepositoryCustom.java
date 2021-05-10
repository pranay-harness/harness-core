package io.harness.repositories.pipeline.custom;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@ToBeDeleted
@Deprecated
public interface NgPipelineRepositoryCustom {
  Page<NgPipelineEntity> findAll(Criteria criteria, Pageable pageable);
  NgPipelineEntity update(Criteria criteria, NgPipelineEntity ngPipelineEntity);
  UpdateResult delete(Criteria criteria);
  List<NgPipelineEntity> findAllWithCriteriaAndProjectOnFields(
      Criteria criteria, @NotNull List<String> includedFields, @NotNull List<String> excludedFields);
}
