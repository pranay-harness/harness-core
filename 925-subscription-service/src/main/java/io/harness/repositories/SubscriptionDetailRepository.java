package io.harness.repositories;

import io.harness.ModuleType;
import io.harness.annotation.HarnessRepo;
import io.harness.subscription.entities.SubscriptionDetail;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface SubscriptionDetailRepository extends CrudRepository<SubscriptionDetail, String> {
  SubscriptionDetail findByAccountIdentifierAndModuleType(String accountIdentifier, ModuleType moduleType);
  List<SubscriptionDetail> findByAccountIdentifier(String accountIdentifier);
  SubscriptionDetail findBySubscriptionId(String subscriptionId);
  long deleteBySubscriptionId(String subscriptionId);
}
