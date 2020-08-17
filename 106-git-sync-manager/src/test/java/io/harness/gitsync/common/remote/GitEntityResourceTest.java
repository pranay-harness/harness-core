package io.harness.gitsync.common.remote;

import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.EntityScope.Scope;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;
import io.harness.gitsync.core.dao.api.repositories.GitFileLocation.GitFileLocationRepository;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

public class GitEntityResourceTest extends GitSyncBaseTest {
  @Inject GitEntityResource gitEntityResource;
  @Inject GitFileLocationRepository gitFileLocationRepository;

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
    final String pipeline = "pipeline";
    final String id = "id";
    final String connector = "connector";
    final String id1 = "id1";
    final GitFileLocation gitFileLocation = GitFileLocation.builder()
                                                .repo(repo)
                                                .branch(branch)
                                                .entityType(pipeline)
                                                .entityIdentifier(id)
                                                .accountId(accountId)
                                                .scope(Scope.ACCOUNT)
                                                .build();
    final GitFileLocation gitFileLocation1 = GitFileLocation.builder()
                                                 .repo(repo)
                                                 .branch(branch)
                                                 .entityType(connector)
                                                 .entityIdentifier(id1)
                                                 .accountId(accountId)
                                                 .scope(Scope.ACCOUNT)
                                                 .build();
    gitFileLocationRepository.saveAll(Arrays.asList(gitFileLocation, gitFileLocation1));

    List<GitSyncEntityListDTO> gitSyncEntityList = gitEntityResource.list(null, null, accountId);

    assertThat(gitSyncEntityList).isNotNull();
    assertThat(gitSyncEntityList.size()).isEqualTo(2);
    assertThat(gitSyncEntityList.get(0).getEntityType()).isEqualTo(pipeline);
    assertThat(gitSyncEntityList.get(1).getEntityType()).isEqualTo(connector);
    assertThat(gitSyncEntityList.get(0).getGitSyncEntities().size()).isEqualTo(1);
    assertThat(gitSyncEntityList.get(1).getGitSyncEntities().get(0).getYamlPath())
        .isEqualTo(connector + PATH_DELIMITER + id1 + EXTENSION_SEPARATOR + YAML_EXTENSION);
    assertThat(gitSyncEntityList.get(0).getGitSyncEntities().get(0).getYamlPath())
        .isEqualTo(pipeline + PATH_DELIMITER + id + EXTENSION_SEPARATOR + YAML_EXTENSION);
  }
}