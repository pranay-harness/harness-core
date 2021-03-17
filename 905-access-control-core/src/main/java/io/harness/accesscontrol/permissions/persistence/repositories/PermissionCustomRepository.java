package io.harness.accesscontrol.permissions.persistence.repositories;

import io.harness.accesscontrol.permissions.persistence.PermissionDBO;

import java.util.List;
import javax.validation.constraints.NotNull;
import org.springframework.data.mongodb.core.query.Criteria;

public interface PermissionCustomRepository {
  List<PermissionDBO> findAll(@NotNull Criteria criteria);
}
