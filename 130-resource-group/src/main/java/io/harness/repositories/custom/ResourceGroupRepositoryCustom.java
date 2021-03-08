package io.harness.repositories.custom;

import io.harness.resourcegroup.model.ResourceGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface ResourceGroupRepositoryCustom {
  Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable);

  boolean update(Criteria criteria, Update update);
}
