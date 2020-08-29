package io.harness.gitsync.core.dao.api.repositories.GitFileLocation;

import io.harness.gitsync.common.beans.GitFileLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

public interface GitFileLocationRepositoryCustom {
  List<String> getDistinctEntityName(Criteria criteria, String field);

  Page<GitFileLocation> getGitFileLocation(Criteria criteria, Pageable pageable);
}
