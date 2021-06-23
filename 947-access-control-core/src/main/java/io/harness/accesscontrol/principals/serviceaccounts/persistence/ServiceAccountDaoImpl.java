package io.harness.accesscontrol.principals.serviceaccounts.persistence;

import static io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDBOMapper.fromDBO;
import static io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDBOMapper.toDBO;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccount;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class ServiceAccountDaoImpl implements ServiceAccountDao {
  private final ServiceAccountRepository serviceAccountRepository;

  @Inject
  public ServiceAccountDaoImpl(ServiceAccountRepository serviceAccountRepository) {
    this.serviceAccountRepository = serviceAccountRepository;
  }

  @Override
  public ServiceAccount createIfNotPresent(ServiceAccount serviceAccount) {
    ServiceAccountDBO serviceAccountDBO = toDBO(serviceAccount);
    Optional<ServiceAccountDBO> savedServiceAccount = serviceAccountRepository.findByIdentifierAndScopeIdentifier(
        serviceAccountDBO.getIdentifier(), serviceAccountDBO.getScopeIdentifier());
    return fromDBO(savedServiceAccount.orElseGet(() -> serviceAccountRepository.save(serviceAccountDBO)));
  }

  @Override
  public PageResponse<ServiceAccount> list(PageRequest pageRequest, String scopeIdentifier) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Page<ServiceAccountDBO> serviceAccountPages =
        serviceAccountRepository.findByScopeIdentifier(scopeIdentifier, pageable);
    return PageUtils.getNGPageResponse(serviceAccountPages.map(ServiceAccountDBOMapper::fromDBO));
  }

  @Override
  public Optional<ServiceAccount> get(String identifier, String scopeIdentifier) {
    return serviceAccountRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .flatMap(u -> Optional.of(fromDBO(u)));
  }

  @Override
  public Optional<ServiceAccount> delete(String identifier, String scopeIdentifier) {
    return serviceAccountRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .stream()
        .findFirst()
        .flatMap(u -> Optional.of(fromDBO(u)));
  }
}
