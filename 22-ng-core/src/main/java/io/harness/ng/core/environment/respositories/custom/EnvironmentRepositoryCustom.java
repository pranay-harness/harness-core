package io.harness.ng.core.environment.respositories.custom;

import com.mongodb.client.result.UpdateResult;
import io.harness.ng.core.environment.beans.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface EnvironmentRepositoryCustom {
  Page<Environment> findAll(Criteria criteria, Pageable pageable);
  UpdateResult upsert(Criteria criteria, Environment environment);
  UpdateResult update(Criteria criteria, Environment environment);
}
