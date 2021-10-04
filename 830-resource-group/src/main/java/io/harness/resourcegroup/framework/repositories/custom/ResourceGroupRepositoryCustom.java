package io.harness.resourcegroup.framework.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceGroup;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface ResourceGroupRepositoryCustom {
  Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable);
  Optional<ResourceGroup> find(Criteria criteria);
  boolean delete(Criteria criteria);
  boolean updateMultiple(Query query, Update update);
}
