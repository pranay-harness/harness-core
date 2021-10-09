package io.harness.ng.core.service.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.service.entity.ServiceEntity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.PIPELINE)
public interface ServiceEntityService {
  ServiceEntity create(ServiceEntity serviceEntity);

  Optional<ServiceEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, boolean deleted);

  // TODO(archit): make it transactional
  ServiceEntity update(ServiceEntity requestService);

  // TODO(archit): make it transactional
  ServiceEntity upsert(ServiceEntity requestService);

  Page<ServiceEntity> list(Criteria criteria, Pageable pageable);

  List<ServiceEntity> listRunTimePermission(Criteria criteria);

  boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier, Long version);

  Page<ServiceEntity> bulkCreate(String accountId, List<ServiceEntity> serviceEntities);

  // Find all services for given accountId + orgId + projectId including deleted services in asc order of creation
  List<ServiceEntity> getAllServices(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  Integer findActiveServicesCountAtGivenTimestamp(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs);

  ServiceEntity find(String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      boolean deleted);
}
