package io.harness.repositories.filters;

import io.harness.connector.entities.ConnectorFilter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface ConnectorFilterCustomRepository {
  Page<ConnectorFilter> findAll(Criteria criteria, Pageable pageable);
}
