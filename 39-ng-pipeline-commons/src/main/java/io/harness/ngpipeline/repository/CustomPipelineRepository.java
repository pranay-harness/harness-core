package io.harness.ngpipeline.repository;

import io.harness.cdng.pipeline.beans.entities.NgPipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;

public interface CustomPipelineRepository {
  Optional<NgPipelineEntity> getPipelineByIdExample(String accountId, String pipelineId);
  Page<NgPipelineEntity> findAll(Criteria criteria, Pageable pageable);
  List<NgPipelineEntity> findAllWithCriteria(Criteria criteria);
}
