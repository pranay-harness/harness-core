package software.wings.service.impl.yaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.beans.yaml.YamlSuccessfulChange.ChangeSource.GIT;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitSuccessFulChangeDetail;
import software.wings.beans.yaml.YamlSuccessfulChange;
import software.wings.beans.yaml.YamlSuccessfulChange.YamlSuccessfulChangeBuilder;
import software.wings.service.intfc.yaml.YamlSuccessfulChangeService;
import software.wings.yaml.gitSync.YamlChangeSet;

public class YamlSuccessfulChangeServiceImplTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String FILE1 = "Setup/Applications/app1/Services/service1.yaml";
  public static final String PROCESSINGCOMMITID = "processingcommitid";
  public static final String COMMITID = "commitid";

  @Inject @Spy YamlSuccessfulChangeService yamlSuccessfulChangeService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }
  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void upsert() {
    final String id = yamlSuccessfulChangeService.upsert(YamlSuccessfulChange.builder()
                                                             .yamlFilePath(FILE1)
                                                             .changeRequestTS(123 * 10000000L)
                                                             .changeSource(GIT.name())
                                                             .changeDetail(getGitSuccessFulChangeDetail())
                                                             .accountId(ACCOUNT_ID)
                                                             .build());

    final YamlSuccessfulChange yamlSuccessfulChange = yamlSuccessfulChangeService.get(ACCOUNT_ID, FILE1);
    assertThat(yamlSuccessfulChange.getUuid()).isEqualTo(id);
    assertThat(yamlSuccessfulChange.getYamlFilePath()).isEqualTo(FILE1);
  }
  private GitSuccessFulChangeDetail getGitSuccessFulChangeDetail() {
    return GitSuccessFulChangeDetail.builder().commitId(COMMITID).processingCommitId(PROCESSINGCOMMITID).build();
  }

  private String createYamlSuccessfulChange(YamlSuccessfulChangeBuilder changeBuilder) {
    return wingsPersistence.save(changeBuilder.accountId(ACCOUNT_ID).build());
  }
  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void updateOnHarnessChangeSet() {
    final YamlChangeSet yamlChangeSet =
        YamlChangeSet.builder()
            .accountId(ACCOUNT_ID)
            .gitToHarness(false)
            .gitFileChanges(ImmutableList.of(getChangeFile(FILE1), getChangeFile("file2")))
            .build();
    yamlChangeSet.setUuid("changesetid");
    yamlSuccessfulChangeService.updateOnHarnessChangeSet(yamlChangeSet);
    verify(yamlSuccessfulChangeService, times(2)).upsert(any(YamlSuccessfulChange.class));
  }

  @NotNull
  private GitFileChange getChangeFile(String filepath) {
    return aGitFileChange().withFilePath(filepath).withAccountId(ACCOUNT_ID).build();
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void updateOnSuccessfulGitChangeProcessing() {
    final GitFileChange changeFile = getChangeFile(FILE1);
    changeFile.setCommitTimeMs(123L);
    changeFile.setCommitId("commitid");
    changeFile.setProcessingCommitId("processingcommitid");
    yamlSuccessfulChangeService.updateOnSuccessfulGitChangeProcessing(changeFile, ACCOUNT_ID);
    verify(yamlSuccessfulChangeService, times(1)).upsert(any(YamlSuccessfulChange.class));
  }
}