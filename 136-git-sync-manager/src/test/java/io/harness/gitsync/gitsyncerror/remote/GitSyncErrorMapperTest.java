package io.harness.gitsync.gitsyncerror.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class GitSyncErrorMapperTest extends GitSyncTestBase {
  private String accountIdentifier;
  private String repoUrl;
  private String branchName;
  private GitSyncErrorType errorType;
  private GitSyncErrorStatus status;
  private String failureReason;
  private EntityType entityType;
  private GitSyncErrorDetails gitSyncErrorDetails;
  private GitSyncError gitSyncError;
  private GitSyncErrorDTO gitSyncErrorDTO;

  @Before
  public void setup() {
    accountIdentifier = randomAlphabetic(10);
    repoUrl = "repoUrl";
    branchName = "A";
    errorType = GitSyncErrorType.GIT_TO_HARNESS;
    status = GitSyncErrorStatus.ACTIVE;
    failureReason = "Invalid yaml";
    entityType = EntityType.CONNECTORS;
    gitSyncErrorDetails =
        GitToHarnessErrorDetails.builder().gitCommitId("commitId").commitMessage("Message").yamlContent(null).build();

    gitSyncError = GitSyncError.builder()
                       .accountIdentifier(accountIdentifier)
                       .repoUrl(repoUrl)
                       .branchName(branchName)
                       .errorType(errorType)
                       .status(status)
                       .failureReason(failureReason)
                       .entityType(entityType)
                       .additionalErrorDetails(gitSyncErrorDetails)
                       .build();

    gitSyncErrorDTO = GitSyncErrorDTO.builder()
                          .accountIdentifier(accountIdentifier)
                          .repoUrl(repoUrl)
                          .branchName(branchName)
                          .errorType(errorType)
                          .status(status)
                          .failureReason(failureReason)
                          .entityType(entityType)
                          .additionalErrorDetails(gitSyncErrorDetails)
                          .build();
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void toGitSyncErrorDTOTest() {
    GitSyncErrorDTO dto = GitSyncErrorMapper.toGitSyncErrorDTO(gitSyncError);
    assertThat(dto).isEqualTo(gitSyncErrorDTO);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void toGitSyncErrorTest() {
    GitSyncError error = GitSyncErrorMapper.toGitSyncError(gitSyncErrorDTO, accountIdentifier);
    assertThat(error).isEqualTo(gitSyncError);
  }
}
