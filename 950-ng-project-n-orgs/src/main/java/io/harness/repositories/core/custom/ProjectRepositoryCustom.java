package io.harness.repositories.core.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Project;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface ProjectRepositoryCustom {
  Page<Project> findAll(Criteria criteria, Pageable pageable);

  Project update(Query query, Update update);

  Project delete(String accountIdentifier, String orgIdentifier, String identifier, Long version);

  List<Project> findAll(Criteria criteria);

  Project restore(String accountIdentifier, String orgIdentifier, String identifier);
}
