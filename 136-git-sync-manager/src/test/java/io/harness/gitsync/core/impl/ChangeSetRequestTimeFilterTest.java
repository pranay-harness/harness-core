package io.harness.gitsync.core.impl;

import static io.harness.gitsync.common.beans.YamlChangeSet.Status.COMPLETED;
import static io.harness.gitsync.common.beans.YamlChangeSet.Status.RUNNING;
import static io.harness.gitsync.core.beans.YamlSuccessfulChange.ChangeSource.GIT;
import static io.harness.gitsync.core.beans.YamlSuccessfulChange.ChangeSource.HARNESS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.git.model.GitFileChange;
import io.harness.git.model.GitFileChange.GitFileChangeBuilder;
import io.harness.gitsync.GitSyncBaseTest;
import io.harness.gitsync.common.beans.YamlChangeSet;
import io.harness.gitsync.core.beans.GitSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.HarnessSuccessFulChangeDetail;
import io.harness.gitsync.core.beans.YamlSuccessfulChange;
import io.harness.gitsync.core.beans.YamlSuccessfulChange.YamlSuccessfulChangeBuilder;
import io.harness.gitsync.core.dao.api.repositories.YamlChangeSet.YamlChangeSetRepository;
import io.harness.gitsync.core.dao.api.repositories.YamlSuccessfulChange.YamlSuccessfulChangeRepository;
import io.harness.gitsync.core.dtos.YamlFilterResult;
import io.harness.gitsync.core.service.YamlChangeSetService;
import io.harness.gitsync.core.service.YamlSuccessfulChangeService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class ChangeSetRequestTimeFilterTest extends GitSyncBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String FILE1 = "Setup/Applications/app1/Services/service1.yaml";
  public static final String FILE2 = "Setup/Applications/app1/Services/service2.yaml";
  public static final String FILE3 = "Setup/Applications/app1/Services/service3.yaml";
  public static final String FILE4 = "Setup/Applications/app1/Services/service4.yaml";
  public static final String PROCESSINGCOMMITID = "processingcommitid";
  public static final String COMMITID = "commitid";
  public static final String CHANGESETID = "changesetid";
  @Inject YamlSuccessfulChangeService yamlSuccessfulChangeService;
  @Inject YamlChangeSetService yamlChangeSetService;

  @Inject YamlChangeSetRepository yamlChangeSetRepository;
  @Inject YamlSuccessfulChangeRepository yamlSuccessfulChangeRepository;
  @Inject ChangeSetRequestTimeFilter changeSetRequestTimeFilter;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_filterFiles() {
    final List<GitFileChange> gitFileChanges = ImmutableList.of(
        createFileToExclude(), createFileToInclude2(), createFileToInclude(), createFileToExcludeScenario2());
    final YamlFilterResult yamlFilterResult =
        changeSetRequestTimeFilter.filterFiles(gitFileChanges, ACCOUNT_ID, YamlGitConfigDTO.builder().build());
    assertThat(yamlFilterResult.getFilteredFiles()).contains(gitFileChanges.get(2), gitFileChanges.get(1));
    assertThat(yamlFilterResult.getExcludedFilePathWithReasonMap().keySet())
        .contains(gitFileChanges.get(0).getFilePath(), gitFileChanges.get(3).getFilePath());
  }

  private GitFileChange createFileToInclude() {
    createYamlSuccessfulChange(YamlSuccessfulChange.builder()
                                   .yamlFilePath(FILE1)
                                   .changeRequestTS(123 * 10000000L)
                                   .changeSource(GIT)
                                   .changeDetail(getGitSuccessFulChangeDetail()));

    return getGitFileChangeBuilder(FILE1).commitTimeMs(200 * 10000000L).build();
  }

  private GitSuccessFulChangeDetail getGitSuccessFulChangeDetail() {
    return GitSuccessFulChangeDetail.builder().commitId(COMMITID).processingCommitId(PROCESSINGCOMMITID).build();
  }

  private GitFileChange createFileToExclude() {
    createYamlSuccessfulChange(YamlSuccessfulChange.builder()
                                   .yamlFilePath(FILE2)
                                   .changeRequestTS(200 * 10000000L)
                                   .changeSource(GIT)
                                   .changeDetail(getGitSuccessFulChangeDetail()));
    return getGitFileChangeBuilder(FILE2).commitTimeMs(198 * 10000000L).build();
  }

  private GitFileChange createFileToExcludeScenario2() {
    final YamlChangeSet yamlChangeSet = YamlChangeSet.builder().status(RUNNING).build();
    yamlChangeSet.setAccountId(ACCOUNT_ID);
    yamlChangeSet.setUuid(CHANGESETID);
    yamlChangeSetRepository.save(yamlChangeSet);

    createYamlSuccessfulChange(
        YamlSuccessfulChange.builder()
            .yamlFilePath(FILE3)
            .changeRequestTS(200 * 10000000L)
            .changeSource(HARNESS)
            .changeDetail(HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(CHANGESETID).build()));
    return getGitFileChangeBuilder(FILE3).commitTimeMs(201 * 10000000L).build();
  }

  private GitFileChange createFileToInclude2() {
    final YamlChangeSet yamlChangeSet = YamlChangeSet.builder().status(COMPLETED).build();
    yamlChangeSet.setAccountId(ACCOUNT_ID);
    yamlChangeSet.setUuid(CHANGESETID + 1);
    yamlChangeSetRepository.save(yamlChangeSet);

    createYamlSuccessfulChange(
        YamlSuccessfulChange.builder()
            .yamlFilePath(FILE4)
            .changeRequestTS(200 * 10000000L)
            .changeSource(HARNESS)
            .changeDetail(HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(CHANGESETID + 1).build()));
    return getGitFileChangeBuilder(FILE4).commitTimeMs(201 * 10000000L).build();
  }

  private String createYamlSuccessfulChange(YamlSuccessfulChangeBuilder changeBuilder) {
    return yamlSuccessfulChangeRepository.save(changeBuilder.accountId(ACCOUNT_ID).build()).getUuid();
  }

  @NotNull
  private GitFileChangeBuilder getGitFileChangeBuilder(String filePath) {
    return GitFileChange.builder().accountId(ACCOUNT_ID).filePath(filePath);
  }
}
