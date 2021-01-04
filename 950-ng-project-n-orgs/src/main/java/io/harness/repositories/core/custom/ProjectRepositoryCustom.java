package io.harness.repositories.core.custom;

import io.harness.ng.core.entities.Project;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface ProjectRepositoryCustom {
  Page<Project> findAll(Criteria criteria, Pageable pageable);

  Project update(Query query, Update update);

  Boolean delete(String accountIdentifier, String orgIdentifier, String identifier, Long version);

  List<Project> findAll(Criteria criteria);
}
