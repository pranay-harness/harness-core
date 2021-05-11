package io.harness.repositories.gitBranches;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitBranch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public interface GitBranchesRepositoryCustom {
  Page<GitBranch> findAll(Criteria criteria, Pageable pageable);

  GitBranch update(Query query, Update update);

  GitBranch findOne(Criteria criteria);
}
