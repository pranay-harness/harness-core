package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.subscription.entities.StripeCustomer;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface StripeCustomerRepository extends CrudRepository<StripeCustomer, String> {
  StripeCustomer findByAccountIdentifierAndCustomerId(String accountIdentifier, String customerId);
  StripeCustomer findByCustomerId(String customerId);
  List<StripeCustomer> findByAccountIdentifier(String accountIdentifier);
}
