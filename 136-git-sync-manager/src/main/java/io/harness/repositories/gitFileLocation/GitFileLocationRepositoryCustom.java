package io.harness.repositories.gitFileLocation;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncRepoFilesDTO;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(DX)
public interface GitFileLocationRepositoryCustom {
  List<String> getDistinctEntityName(Criteria criteria, String field);

  Page<GitFileLocation> getGitFileLocation(Criteria criteria, Pageable pageable);

  List<GitSyncRepoFilesDTO> getByProjectIdAndOrganizationIdAndAccountIdAndGitSyncConfigIdentifierListAndEntityTypeList(
      String projectIdentifier, String orgIdentifier, String accountIdentifier,
      List<String> gitSyncConfigIdentifierList, List<EntityType> entityTypeList, String searchTerm, int size);

  List<GitSyncEntityListDTO> getByProjectIdAndOrganizationIdAndAccountIdAndRepoUrlAndEntityTypeListAndBranch(
      String projectIdentifier, String orgIdentifier, String accountIdentifier, String gitSyncConfigIdentifier,
      String branch, List<EntityType> entityTypeList, String searchTerm, int size);
}
