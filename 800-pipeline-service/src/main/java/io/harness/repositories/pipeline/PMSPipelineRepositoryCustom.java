package io.harness.repositories.pipeline;

import io.harness.pms.pipeline.PipelineEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface PMSPipelineRepositoryCustom {
  PipelineEntity update(Criteria criteria, PipelineEntity pipelineEntity);

  PipelineEntity update(Criteria criteria, Update update);

  UpdateResult delete(Criteria criteria);

  Page<PipelineEntity> findAll(Criteria criteria, Pageable pageable);
}
