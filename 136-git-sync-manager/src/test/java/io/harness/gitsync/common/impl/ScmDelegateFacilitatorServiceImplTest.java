/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.HARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.delegate.task.scm.GitFileTaskResponseData;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitFileContent;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.ng.beans.PageRequest;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class ScmDelegateFacilitatorServiceImplTest extends GitSyncTestBase {
  @Mock SecretManagerClientService secretManagerClientService;
  @Mock ConnectorService connectorService;
  @Mock DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock AbstractScmClientFacilitatorServiceImpl abstractScmClientFacilitatorService;
  @Mock YamlGitConfigService yamlGitConfigService;
  @InjectMocks @Inject ScmDelegateFacilitatorServiceImpl scmDelegateFacilitatorService;
  FileContent fileContent = FileContent.newBuilder().build();
  String accountIdentifier = "accountIdentifier";
  String projectIdentifier = "projectIdentifier";
  String orgIdentifier = "orgIdentifier";
  String connectorIdentifierRef = "connectorIdentifierRef";
  String repoURL = "repoURL";
  String yamlGitConfigIdentifier = "yamlGitConfigIdentifier";
  String filePath = "filePath";
  String branch = "branch";
  final ListBranchesResponse listBranchesResponse =
      ListBranchesResponse.newBuilder().addBranches("master").addBranches("feature").build();

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(secretManagerClientService.getEncryptionDetails(any(), any())).thenReturn(Collections.emptyList());
    GithubConnectorDTO githubConnector =
        GithubConnectorDTO.builder().apiAccess(GithubApiAccessDTO.builder().build()).build();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder().connectorConfig(githubConnector).build();
    doReturn(Optional.of(ConnectorResponseDTO.builder().connector(connectorInfo).build()))
        .when(connectorService)
        .get(anyString(), anyString(), anyString(), anyString());
    when(abstractScmClientFacilitatorService.getYamlGitConfigDTO(
             accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier))
        .thenReturn(YamlGitConfigDTO.builder().build());
    when(abstractScmClientFacilitatorService.getConnectorIdentifierRef(any(), anyString(), anyString(), anyString()))
        .thenReturn(IdentifierRef.builder().build());
    when(yamlGitConfigService.get(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(YamlGitConfigDTO.builder()
                        .accountIdentifier(accountIdentifier)
                        .projectIdentifier(projectIdentifier)
                        .organizationIdentifier(orgIdentifier)
                        .gitConnectorRef(connectorIdentifierRef)
                        .build());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void listBranchesForRepoByConnectorTest() {
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(
            ScmGitRefTaskResponseData.builder().listBranchesResponse(listBranchesResponse.toByteArray()).build());
    final List<String> branches = scmDelegateFacilitatorService.listBranchesForRepoByConnector(accountIdentifier,
        orgIdentifier, projectIdentifier, connectorIdentifierRef, repoURL,
        PageRequest.builder().pageIndex(0).pageSize(10).build(), "");
    assertThat(branches).isEqualTo(listBranchesResponse.getBranchesList());
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void getFileContentTest() {
    when(delegateGrpcClientWrapper.executeSyncTask(any()))
        .thenReturn(GitFileTaskResponseData.builder().fileContent(fileContent.toByteArray()).build());
    final GitFileContent gitFileContent = scmDelegateFacilitatorService.getFileContent(
        yamlGitConfigIdentifier, accountIdentifier, orgIdentifier, projectIdentifier, filePath, branch, null);
    assertThat(gitFileContent)
        .isEqualTo(
            GitFileContent.builder().content(fileContent.getContent()).objectId(fileContent.getBlobId()).build());
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testCreatePRWithSameSourceTargetBranch() {
    GitPRCreateRequest createPRRequest =
        GitPRCreateRequest.builder().sourceBranch("branch").targetBranch("branch").build();
    assertThatThrownBy(() -> scmDelegateFacilitatorService.createPullRequest(createPRRequest))
        .isInstanceOf(InvalidRequestException.class);
  }
}
