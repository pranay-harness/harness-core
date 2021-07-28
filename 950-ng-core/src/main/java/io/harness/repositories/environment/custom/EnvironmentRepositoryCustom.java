package io.harness.repositories.environment.custom;

import io.harness.ng.core.environment.beans.Environment;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface EnvironmentRepositoryCustom {
  Page<Environment> findAll(Criteria criteria, Pageable pageable);
  Environment upsert(Criteria criteria, Environment environment);
  Environment update(Criteria criteria, Environment environment);
  UpdateResult delete(Criteria criteria);

  List<Environment> findAllRunTimeAccess(Criteria criteria);
}
