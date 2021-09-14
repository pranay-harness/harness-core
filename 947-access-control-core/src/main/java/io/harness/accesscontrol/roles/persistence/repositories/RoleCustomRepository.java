/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PL)
public interface RoleCustomRepository {
  Page<RoleDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);

  Optional<RoleDBO> find(@NotNull Criteria criteria);

  UpdateResult updateMulti(@NotNull Criteria criteria, @NotNull Update update);

  long deleteMulti(@NotNull Criteria criteria);
}
