package io.harness.ng.core.services;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface OrganizationService {
  Organization create(String accountIdentifier, OrganizationDTO organization);

  List<String> getDistinctAccounts();

  Optional<Organization> get(String accountIdentifier, String identifier);

  Organization update(String accountIdentifier, String identifier, OrganizationDTO organization);

  /**
   * Use this method with caution, verify that the pageable sort is able to make use of the indexes.
   */
  Page<Organization> listPermittedOrgs(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO);

  /**
   * Use this method with caution, verify that the criteria and pageable sort is able to make use of the indexes.
   */
  Page<Organization> list(Criteria criteria, Pageable pageable);

  /**
   * Use this method with caution, verify that the criteria is able to make use of the indexes.
   */
  List<Organization> list(Criteria criteria);

  boolean delete(String accountIdentifier, String identifier, Long version);

  boolean restore(String accountIdentifier, String identifier);
}
