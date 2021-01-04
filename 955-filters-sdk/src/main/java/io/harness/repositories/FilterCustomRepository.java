package io.harness.repositories;

import io.harness.filter.entity.Filter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface FilterCustomRepository {
  Page<Filter> findAll(Criteria criteria, Pageable pageable);
}
