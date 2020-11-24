package io.harness.repositories.ng.core.custom;

import io.harness.ng.core.models.Secret;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
public class SecretRepositoryCustomImpl implements SecretRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<Secret> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<Secret> projects = mongoTemplate.find(query, Secret.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Secret.class));
  }
}
