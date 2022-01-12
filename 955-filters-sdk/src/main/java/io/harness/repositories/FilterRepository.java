/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.filter.entity.Filter;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
@OwnedBy(DX)
public interface FilterRepository extends FilterCustomRepository, PagingAndSortingRepository<Filter, String> {
  long deleteByFullyQualifiedIdentifierAndFilterType(String fullyQualifiedIdentifier, FilterType filterType);

  Optional<Filter> findByFullyQualifiedIdentifierAndFilterType(String fullyQualifiedIdentifier, FilterType filterType);

  Optional<Filter> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndName(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String name);
}
