package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;

import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface RoleCustomRepository {
  Page<RoleDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);
}
