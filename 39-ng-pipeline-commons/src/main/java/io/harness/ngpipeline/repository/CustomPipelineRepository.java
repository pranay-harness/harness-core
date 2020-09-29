package io.harness.ngpipeline.repository;

import io.harness.cdng.pipeline.beans.entities.CDPipelineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;

public interface CustomPipelineRepository {
  Optional<CDPipelineEntity> getPipelineByIdExample(String accountId, String pipelineId);
  Page<CDPipelineEntity> findAll(Criteria criteria, Pageable pageable);
  List<CDPipelineEntity> findAllWithCriteria(Criteria criteria);
}
