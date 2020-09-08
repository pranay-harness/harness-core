package io.harness.cdng.inputset.repository.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface CDInputSetRepositoryCustom {
  Page<CDInputSetEntity> findAll(Criteria criteria, Pageable pageable);
  UpdateResult upsert(Criteria criteria, CDInputSetEntity cdInputSetEntity);
  UpdateResult update(Criteria criteria, CDInputSetEntity cdInputSetEntity);
  UpdateResult delete(Criteria criteria);
}
