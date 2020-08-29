package io.harness.gitsync.gitsyncerror.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.gitsync.gitsyncerror.dao.api.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class GitSyncErrorServiceImpl implements GitSyncErrorService {
  private GitSyncErrorRepository gitSyncErrorRepository;

  @Override
  public void deleteByAccountIdOrgIdProjectIdAndFilePath(
      String accountId, String orgId, String projectId, List<String> yamlFilePath) {
    gitSyncErrorRepository.removeByAccountIdAndOrganizationIdAndProjectIdAndYamlFilePathIn(
        accountId, orgId, projectId, yamlFilePath);
  }
}
