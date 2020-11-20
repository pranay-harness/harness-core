package io.harness.gitsync.common.remote;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.common.dtos.GitSyncProductDTO;
import io.harness.gitsync.common.impl.GitEntityServiceImpl;
import io.harness.gitsync.core.dao.api.repositories.GitFileLocation.GitFileLocationRepository;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GitEntityResourceTest extends GitSyncBaseTest {
  @Inject GitEntityResource gitEntityResource;
  @Inject GitFileLocationRepository gitFileLocationRepository;
  @Inject GitEntityServiceImpl gitEntityService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testList() {
    final String repo = "repo";
    final String branch = "branch";
    final String accountId = "accountId";
    final String pipeline = EntityType.PIPELINES.getYamlName();
    final String id = "id";
    final String connector = EntityType.CONNECTORS.getYamlName();
    final String id1 = "id1";
    final GitFileLocation gitFileLocation = buildGitFileLocation(repo, branch, accountId, pipeline, id);
    final GitFileLocation gitFileLocation1 = buildGitFileLocation(repo, branch, accountId, connector, id1);
    gitFileLocationRepository.saveAll(Arrays.asList(gitFileLocation, gitFileLocation, gitFileLocation1));

    final ResponseDTO<GitSyncProductDTO> gitSyncEntities =
        gitEntityResource.list(null, null, accountId, 5, ModuleType.CD);
    final GitSyncProductDTO data = gitSyncEntities.getData();

    assertThat(data).isNotNull();
    assertThat(data.getGitSyncEntityListDTOList()
                   .stream()
                   .map(GitSyncEntityListDTO::getEntityType)
                   .collect(Collectors.toList()))
        .isEqualTo(gitEntityService.getEntityTypesFromModuleType(ModuleType.CD));
    assertThat(data.getGitSyncEntityListDTOList()
                   .stream()
                   .flatMap(gitSyncEntityListDTO -> gitSyncEntityListDTO.getGitSyncEntities().stream())
                   .collect(Collectors.toList())
                   .size())
        .isEqualTo(2);
    assertThat(
        data.getGitSyncEntityListDTOList().stream().map(GitSyncEntityListDTO::getCount).collect(Collectors.toList()))
        .isEqualTo(Arrays.asList(2L));
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testListByType() {
    final String repo = "repo";
    final String branch = "branch";
    final String accountId = "accountId";
    final String pipeline = EntityType.PIPELINES.getYamlName();
    final String id = "id";
    final String connector = EntityType.CONNECTORS.getYamlName();
    final String id1 = "id1";
    final GitFileLocation gitFileLocation = buildGitFileLocation(repo, branch, accountId, pipeline, id);
    final GitFileLocation gitFileLocation1 = buildGitFileLocation(repo, branch, accountId, connector, id1);
    gitFileLocationRepository.saveAll(Arrays.asList(gitFileLocation, gitFileLocation, gitFileLocation1));

    final ResponseDTO<PageResponse<GitSyncEntityListDTO>> ngPageResponseResponseDTO =
        gitEntityResource.listByType(null, null, accountId, EntityType.PIPELINES, 0, 5, "cd");
    final PageResponse<GitSyncEntityListDTO> data = ngPageResponseResponseDTO.getData();
    assertThat(data).isNotNull();
    assertThat(data.getContent()
                   .stream()
                   .flatMap(gitSyncEntityListDTO -> gitSyncEntityListDTO.getGitSyncEntities().stream())
                   .collect(Collectors.toList())
                   .size())
        .isEqualTo(2);
  }

  public GitFileLocation buildGitFileLocation(
      String repo, String branch, String accountId, String pipeline, String id) {
    return GitFileLocation.builder()
        .repo(repo)
        .branch(branch)
        .entityType(pipeline)
        .entityIdentifier(id)
        .accountId(accountId)
        .scope(Scope.ACCOUNT)
        .build();
  }
}
