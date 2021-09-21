package io.harness.resourcegroup.framework.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceGroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface ResourceGroupRepositoryCustom {
  Page<ResourceGroup> findAll(Criteria criteria, Pageable pageable);
}
