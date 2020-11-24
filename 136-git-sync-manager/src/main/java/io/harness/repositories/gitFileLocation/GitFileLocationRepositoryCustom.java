package io.harness.repositories.gitFileLocation;

import io.harness.gitsync.common.beans.GitFileLocation;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface GitFileLocationRepositoryCustom {
  List<String> getDistinctEntityName(Criteria criteria, String field);

  Page<GitFileLocation> getGitFileLocation(Criteria criteria, Pageable pageable);
}
