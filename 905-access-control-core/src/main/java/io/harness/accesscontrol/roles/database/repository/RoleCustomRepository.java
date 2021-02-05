package io.harness.accesscontrol.roles.database.repository;

import io.harness.accesscontrol.roles.database.Role;

import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface RoleCustomRepository {
  Page<Role> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);
}
