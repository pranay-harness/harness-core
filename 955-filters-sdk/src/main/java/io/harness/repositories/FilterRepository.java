package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.filter.FilterType;
import io.harness.filter.entity.Filter;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
public interface FilterRepository extends FilterCustomRepository, PagingAndSortingRepository<Filter, String> {
  long deleteByFullyQualifiedIdentifierAndFilterType(String fullyQualifiedIdentifier, FilterType filterType);
  Optional<Filter> findByFullyQualifiedIdentifierAndFilterType(String fullyQualifiedIdentifier, FilterType filterType);
  Optional<Filter> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndName(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String name);
}