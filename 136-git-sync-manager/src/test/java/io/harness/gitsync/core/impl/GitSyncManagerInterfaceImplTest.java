package io.harness.gitsync.core.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.common.YamlConstants.EXTENSION_SEPARATOR;
import static io.harness.gitsync.common.YamlConstants.PATH_DELIMITER;
import static io.harness.gitsync.common.YamlConstants.YAML_EXTENSION;
import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.ChangeType;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(DX)
public class GitSyncManagerInterfaceImplTest extends GitSyncTestBase {
  public static final String ACCOUNT_ID = "accountId";
  public static final String FOLDER_NAME = "folderName";
  public static final String ENTITY_TYPE = "pipeline";
  public static final String ENTITY_IDENTIFIER = "pipelineId";
  public static final String YAML_CONTENT = "content";
  @Inject YamlChangeSetService yamlChangeSetService;

  @Mock YamlGitConfigService yamlGitConfigService;

  @InjectMocks @Inject GitSyncManagerInterfaceImpl gitSyncManagerInterface;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  @Ignore("Will bring it back once the ng git sync work is complete")
  public void testProcessHarnessToGit() {
    YamlGitConfigDTO.RootFolder rootFolder =
        YamlGitConfigDTO.RootFolder.builder().enabled(true).identifier("abcd").rootFolder(FOLDER_NAME).build();
    final YamlGitConfigDTO yamlGitConfigDTO = YamlGitConfigDTO.builder()
                                                  .accountIdentifier(ACCOUNT_ID)
                                                  .rootFolders(Arrays.asList(rootFolder))
                                                  .defaultRootFolder(rootFolder)
                                                  .build();
    String yamlChangeSetId = gitSyncManagerInterface.processHarnessToGit(
        ChangeType.ADD, YAML_CONTENT, ACCOUNT_ID, null, null, "pipelineName", ENTITY_TYPE, ENTITY_IDENTIFIER);
    Optional<YamlChangeSet> yamlChangeSet = yamlChangeSetService.get(ACCOUNT_ID, yamlChangeSetId);
    assertThat(yamlChangeSet.isPresent()).isTrue();
    assertThat(yamlChangeSet.get().getAccountId()).isEqualTo(ACCOUNT_ID);
    List<GitFileChange> gitFileChanges = yamlChangeSet.get().getGitFileChanges();
    assertThat(gitFileChanges.size()).isEqualTo(1);
    GitFileChange gitFileChange = gitFileChanges.get(0);
    assertThat(gitFileChange.getFilePath())
        .isEqualTo(FOLDER_NAME + PATH_DELIMITER + ENTITY_TYPE + PATH_DELIMITER + ENTITY_IDENTIFIER + EXTENSION_SEPARATOR
            + YAML_EXTENSION);
    assertThat(gitFileChange.getFileContent()).isEqualTo(YAML_CONTENT);
  }
}
