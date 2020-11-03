package io.harness.ng.core.service.services;

import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;

public interface ServiceEntityService {
  ServiceEntity create(ServiceEntity serviceEntity);

  Optional<ServiceEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean deleted);

  // TODO(archit): make it transactional
  ServiceEntity update(ServiceEntity requestService);

  // TODO(archit): make it transactional
  ServiceEntity upsert(ServiceEntity requestService);

  Page<ServiceEntity> list(Criteria criteria, Pageable pageable);

  boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, Long version);

  Page<ServiceEntity> bulkCreate(String accountId, List<ServiceEntity> serviceEntities);
}
