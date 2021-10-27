package io.harness.gitsync.common.service;

import io.harness.ng.beans.PageRequest;

import java.util.List;

public interface ScmFacilitatorService {
  List<String> listBranchesUsingConnector(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifierRef, String repoURL, PageRequest pageRequest, String searchTerm);
}
