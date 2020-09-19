package io.harness.app.dao.repositories;

import io.harness.beans.CIPipeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface CIPipelineRepositoryCustom {
  Page<CIPipeline> findAll(Criteria criteria, Pageable pageable);
  List<CIPipeline> findAllWithCriteria(Criteria criteria);
}
