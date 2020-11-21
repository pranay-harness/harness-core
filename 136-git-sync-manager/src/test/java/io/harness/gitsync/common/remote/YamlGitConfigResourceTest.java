package io.harness.gitsync.common.remote;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.gitsync.common.dtos.GitSyncFolderConfigDTO;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class YamlGitConfigResourceTest extends GitSyncBaseTest {
  @Inject YamlGitConfigResource yamlGitConfigResource;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String ORG_ID = "ORG_ID";
  private final String PROJECT_ID = "PROJECT_ID";
  private final String IDENTIFIER = "ID";
  private final String IDENTIFIER_1 = "ID_1";
  private final String CONNECTOR_ID = "CONNECTOR_ID";
  private final String CONNECTOR_ID_1 = "CONNECTOR_ID_1";
  private final String REPO = "REPO";
  private final String BRANCH = "BRANCH";
  private final String ROOT_FOLDER = "ROOT_FOLDER";
  private final String ROOT_FOLDER_ID = "ROOT_FOLDER_ID";
  private final String ROOT_FOLDER_1 = "ROOT_FOLDER_1";
  private final String ROOT_FOLDER_ID_1 = "ROOT_FOLDER_ID_1";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void test_save() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(true)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncConfigDTO gitSyncConfigDTO =
        buildGitSyncDTO(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    GitSyncConfigDTO ret = yamlGitConfigResource.create(PROJECT_ID, ORG_ID, ACCOUNT_ID, gitSyncConfigDTO);
    assertThat(ret).isEqualTo(gitSyncConfigDTO);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSaveWithDefaultOverride() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(true)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncFolderConfigDTO rootFolder_1 = GitSyncFolderConfigDTO.builder()
                                              .isDefault(true)
                                              .rootFolder(ROOT_FOLDER_1)
                                              .identifier(ROOT_FOLDER_ID_1)
                                              .enabled(true)
                                              .build();
    saveYamlGitConfig(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    saveYamlGitConfig(Collections.singletonList(rootFolder_1), CONNECTOR_ID_1, REPO, BRANCH, IDENTIFIER_1);

    List<GitSyncConfigDTO> gitSyncConfigDTOS = yamlGitConfigResource.list(PROJECT_ID, ORG_ID, ACCOUNT_ID);
    assertThat(getDefault(gitSyncConfigDTOS)).isEqualTo(rootFolder_1);
  }

  private GitSyncConfigDTO buildGitSyncDTO(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    return GitSyncConfigDTO.builder()
        .accountId(ACCOUNT_ID)
        .organizationId(ORG_ID)
        .projectId(PROJECT_ID)
        .repo(repo)
        .branch(branch)
        .gitConnectorId(connectorId)
        .identifier(identifier)
        .gitSyncFolderConfigDTOs(rootFolder)
        .build();
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testConnectorUpdate() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(true)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncConfigDTO gitSyncConfigDTO =
        saveYamlGitConfig(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    gitSyncConfigDTO.setGitConnectorId(CONNECTOR_ID_1);
    GitSyncConfigDTO ret = yamlGitConfigResource.update(PROJECT_ID, ORG_ID, ACCOUNT_ID, IDENTIFIER, gitSyncConfigDTO);
    assertThat(ret.getGitConnectorId()).isEqualTo(CONNECTOR_ID_1);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testFolderAdditionInUpdate() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(true)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncFolderConfigDTO rootFolder_1 = GitSyncFolderConfigDTO.builder()
                                              .isDefault(false)
                                              .rootFolder(ROOT_FOLDER_1)
                                              .identifier(ROOT_FOLDER_ID_1)
                                              .enabled(true)
                                              .build();
    GitSyncConfigDTO gitSyncConfigDTO =
        saveYamlGitConfig(Collections.singletonList(rootFolder), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    gitSyncConfigDTO.setGitSyncFolderConfigDTOs(Arrays.asList(rootFolder, rootFolder_1));
    GitSyncConfigDTO ret = yamlGitConfigResource.update(PROJECT_ID, ORG_ID, ACCOUNT_ID, IDENTIFIER, gitSyncConfigDTO);
    assertThat(ret.getGitSyncFolderConfigDTOs().size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUpdateDefault() {
    GitSyncFolderConfigDTO rootFolder = GitSyncFolderConfigDTO.builder()
                                            .isDefault(false)
                                            .rootFolder(ROOT_FOLDER)
                                            .identifier(ROOT_FOLDER_ID)
                                            .enabled(true)
                                            .build();
    GitSyncFolderConfigDTO rootFolder_1 = GitSyncFolderConfigDTO.builder()
                                              .isDefault(false)
                                              .rootFolder("random")
                                              .identifier("random")
                                              .enabled(true)
                                              .build();
    GitSyncFolderConfigDTO rootFolder_2 = GitSyncFolderConfigDTO.builder()
                                              .isDefault(true)
                                              .rootFolder(ROOT_FOLDER_1)
                                              .identifier(ROOT_FOLDER_ID_1)
                                              .enabled(true)
                                              .build();
    saveYamlGitConfig(Arrays.asList(rootFolder, rootFolder_1), CONNECTOR_ID, REPO, BRANCH, IDENTIFIER);
    GitSyncConfigDTO gitSyncConfigDTO =
        saveYamlGitConfig(Arrays.asList(rootFolder_2), CONNECTOR_ID_1, REPO, BRANCH, IDENTIFIER_1);
    assertThat(getDefault(Collections.singletonList(gitSyncConfigDTO))).isEqualTo(rootFolder_2);
    List<GitSyncConfigDTO> gitSyncConfigDTO_ret =
        yamlGitConfigResource.updateDefault(PROJECT_ID, ORG_ID, ACCOUNT_ID, IDENTIFIER, ROOT_FOLDER_ID);
    assertThat(getDefault(gitSyncConfigDTO_ret).getIdentifier()).isEqualTo(ROOT_FOLDER_ID);
  }

  private GitSyncFolderConfigDTO getDefault(List<GitSyncConfigDTO> gitSyncConfigDTOS) {
    Optional<GitSyncFolderConfigDTO> defaultGitSync =
        gitSyncConfigDTOS.stream()
            .map(GitSyncConfigDTO::getGitSyncFolderConfigDTOs)
            .flatMap(gitSyncFolderDTOS -> gitSyncFolderDTOS.stream().filter(GitSyncFolderConfigDTO::getIsDefault))
            .findFirst();
    return defaultGitSync.orElse(null);
  }

  private GitSyncConfigDTO saveYamlGitConfig(
      List<GitSyncFolderConfigDTO> rootFolder, String connectorId, String repo, String branch, String identifier) {
    GitSyncConfigDTO gitSyncConfigDTO = buildGitSyncDTO(rootFolder, connectorId, repo, branch, identifier);
    return yamlGitConfigResource.create(PROJECT_ID, ORG_ID, ACCOUNT_ID, gitSyncConfigDTO);
  }
}
