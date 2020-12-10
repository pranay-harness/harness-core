package io.harness.repositories.invites.custom;

import io.harness.ng.core.invites.entities.UserProjectMap;

import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;

public interface UserProjectMapRepositoryCustom {
  List<UserProjectMap> findAll(Criteria criteria);
}
