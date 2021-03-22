package io.harness.repositories.gitFileLocation;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitFileLocation;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public interface GitFileLocationRepositoryCustom {
  List<String> getDistinctEntityName(Criteria criteria, String field);

  Page<GitFileLocation> getGitFileLocation(Criteria criteria, Pageable pageable);
}
