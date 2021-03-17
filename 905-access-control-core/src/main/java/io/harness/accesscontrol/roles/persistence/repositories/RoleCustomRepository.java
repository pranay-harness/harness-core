package io.harness.accesscontrol.roles.persistence.repositories;

import io.harness.accesscontrol.roles.persistence.RoleDBO;

import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface RoleCustomRepository {
  Page<RoleDBO> findAll(@NotNull Criteria criteria, @NotNull Pageable pageable);

  Optional<RoleDBO> find(@NotNull Criteria criteria);

  UpdateResult updateMulti(@NotNull Criteria criteria, @NotNull Update update);

  long deleteMulti(@NotNull Criteria criteria);
}
