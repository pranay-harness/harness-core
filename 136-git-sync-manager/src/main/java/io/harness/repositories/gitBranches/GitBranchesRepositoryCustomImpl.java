package io.harness.repositories.gitBranches;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitBranch;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class GitBranchesRepositoryCustomImpl implements GitBranchesRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<GitBranch> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    query.collation(Collation.of("en").strength(Collation.ComparisonLevel.secondary()));
    List<GitBranch> projects = mongoTemplate.find(query, GitBranch.class);
    return PageableExecutionUtils.getPage(
        projects, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GitBranch.class));
  }

  @Override
  public GitBranch update(Query query, Update update) {
    return mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true), GitBranch.class);
  }
}
