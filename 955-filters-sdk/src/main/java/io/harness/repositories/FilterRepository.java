/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
