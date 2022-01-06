/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.gitdiff;

import static software.wings.beans.yaml.GitFileChange.Builder.aGitFileChange;
import static software.wings.beans.yaml.YamlSuccessfulChange.ChangeSource.GIT;
import static software.wings.beans.yaml.YamlSuccessfulChange.ChangeSource.HARNESS;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.COMPLETED;
import static software.wings.yaml.gitSync.YamlChangeSet.Status.RUNNING;
import static software.wings.yaml.gitSync.YamlChangeSet.builder;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.beans.yaml.GitSuccessFulChangeDetail;
import software.wings.beans.yaml.HarnessSuccessFulChangeDetail;
import software.wings.beans.yaml.YamlSuccessfulChange;
import software.wings.beans.yaml.YamlSuccessfulChange.YamlSuccessfulChangeBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlSuccessfulChangeService;
import software.wings.yaml.gitSync.YamlChangeSet;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ChangeSetRequestTimeFilterTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String FILE1 = "Setup/Applications/app1/Services/service1.yaml";
  public static final String FILE2 = "Setup/Applications/app1/Services/service2.yaml";
  public static final String FILE3 = "Setup/Applications/app1/Services/service3.yaml";
  public static final String FILE4 = "Setup/Applications/app1/Services/service4.yaml";
  public static final String PROCESSINGCOMMITID = "processingcommitid";
  public static final String COMMITID = "commitid";
  public static final String CHANGESETID = "changesetid";
  @Inject YamlSuccessfulChangeService yamlSuccessfulChangeService;
  @Inject WingsPersistence wingsPersistence;
  @Inject YamlChangeSetService yamlChangeSetService;

  @InjectMocks @Inject ChangeSetRequestTimeFilter changeSetRequestTimeFilter;

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
    final YamlFilterResult yamlFilterResult = changeSetRequestTimeFilter.filterFiles(gitFileChanges, ACCOUNT_ID);
    assertThat(yamlFilterResult.getFilteredFiles()).contains(gitFileChanges.get(2), gitFileChanges.get(1));
    assertThat(yamlFilterResult.getExcludedFilePathWithReasonMap().keySet())
        .contains(gitFileChanges.get(0).getFilePath(), gitFileChanges.get(3).getFilePath());
  }

  private GitFileChange createFileToInclude() {
    createYamlSuccessfulChange(YamlSuccessfulChange.builder()
                                   .yamlFilePath(FILE1)
                                   .changeRequestTS(123 * 10000000L)
                                   .changeSource(GIT.name())
                                   .changeDetail(getGitSuccessFulChangeDetail()));

    return getGitFileChangeBuilder(FILE1).withCommitTimeMs(200 * 10000000L).build();
  }

  private GitSuccessFulChangeDetail getGitSuccessFulChangeDetail() {
    return GitSuccessFulChangeDetail.builder().commitId(COMMITID).processingCommitId(PROCESSINGCOMMITID).build();
  }

  private GitFileChange createFileToExclude() {
    createYamlSuccessfulChange(YamlSuccessfulChange.builder()
                                   .yamlFilePath(FILE2)
                                   .changeRequestTS(200 * 10000000L)
                                   .changeSource(GIT.name())
                                   .changeDetail(getGitSuccessFulChangeDetail()));
    return getGitFileChangeBuilder(FILE2).withCommitTimeMs(198 * 10000000L).build();
  }

  private GitFileChange createFileToExcludeScenario2() {
    final YamlChangeSet yamlChangeSet = builder().status(RUNNING).build();
    yamlChangeSet.setAccountId(ACCOUNT_ID);
    yamlChangeSet.setUuid(CHANGESETID);
    wingsPersistence.save(yamlChangeSet);

    createYamlSuccessfulChange(
        YamlSuccessfulChange.builder()
            .yamlFilePath(FILE3)
            .changeRequestTS(200 * 10000000L)
            .changeSource(HARNESS.name())
            .changeDetail(HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(CHANGESETID).build()));
    return getGitFileChangeBuilder(FILE3).withCommitTimeMs(201 * 10000000L).build();
  }

  private GitFileChange createFileToInclude2() {
    final YamlChangeSet yamlChangeSet = builder().status(COMPLETED).build();
    yamlChangeSet.setAccountId(ACCOUNT_ID);
    yamlChangeSet.setUuid(CHANGESETID + 1);
    wingsPersistence.save(yamlChangeSet);

    createYamlSuccessfulChange(
        YamlSuccessfulChange.builder()
            .yamlFilePath(FILE4)
            .changeRequestTS(200 * 10000000L)
            .changeSource(HARNESS.name())
            .changeDetail(HarnessSuccessFulChangeDetail.builder().yamlChangeSetId(CHANGESETID + 1).build()));
    return getGitFileChangeBuilder(FILE4).withCommitTimeMs(201 * 10000000L).build();
  }

  private String createYamlSuccessfulChange(YamlSuccessfulChangeBuilder changeBuilder) {
    return wingsPersistence.save(changeBuilder.accountId(ACCOUNT_ID).build());
  }

  @NotNull
  private Builder getGitFileChangeBuilder(String filePath) {
    return aGitFileChange().withAccountId(ACCOUNT_ID).withFilePath(filePath);
  }
}
