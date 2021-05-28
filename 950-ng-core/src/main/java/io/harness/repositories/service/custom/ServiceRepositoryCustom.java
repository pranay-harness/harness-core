package io.harness.repositories.service.custom;

import io.harness.ng.core.service.entity.ServiceEntity;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ServiceRepositoryCustom {
  Page<ServiceEntity> findAll(Criteria criteria, Pageable pageable);
  ServiceEntity upsert(Criteria criteria, ServiceEntity serviceEntity);
  ServiceEntity update(Criteria criteria, ServiceEntity serviceEntity);
  UpdateResult delete(Criteria criteria);
  Long findActiveServiceCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);
}
