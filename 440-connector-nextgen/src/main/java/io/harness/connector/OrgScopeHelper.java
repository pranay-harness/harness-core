/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.entities.Organization.OrganizationKeys;

import io.harness.connector.entities.Connector;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class OrgScopeHelper {
  OrganizationService organizationService;

  public Map<String, String> createOrgIdentifierOrgNameMap(List<String> orgIdentifierList) {
    if (isEmpty(orgIdentifierList)) {
      return Collections.emptyMap();
    }
    Criteria criteria = new Criteria();
    criteria.and(OrganizationKeys.identifier).in(orgIdentifierList);
    List<Organization> organizations = organizationService.list(criteria, Pageable.unpaged()).toList();
    if (isEmpty(organizations)) {
      return Collections.emptyMap();
    }
    return organizations.stream().collect(Collectors.toMap(Organization::getIdentifier, Organization::getName));
  }

  public List<String> getOrgIdentifiers(List<Connector> connectors) {
    if (isEmpty(connectors)) {
      return Collections.emptyList();
    }
    return connectors.stream().map(Connector::getOrgIdentifier).collect(Collectors.toList());
  }
}
