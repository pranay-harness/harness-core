package io.harness.ng.core.service.respositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.respositories.custom.ServiceRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@HarnessRepo
public interface ServiceRepository extends PagingAndSortingRepository<ServiceEntity, String>, ServiceRepositoryCustom {
  Optional<ServiceEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      boolean notDeleted);
}
