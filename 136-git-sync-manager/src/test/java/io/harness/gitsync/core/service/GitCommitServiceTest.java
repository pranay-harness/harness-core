package io.harness.gitsync.core.service;

import static io.harness.rule.OwnerRule.ABHINAV;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.core.impl.GitCommitServiceImpl;
import io.harness.repositories.gitCommit.GitCommitRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitCommitServiceTest extends CategoryTest {
  @Mock GitCommitRepository gitCommitRepository;
  @InjectMocks @Inject GitCommitServiceImpl gitCommitService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testSave() {
    // TODO add a fix here
    //    gitCommitService.save(GitCommitDTO.builder().build());
    //    verify(gitCommitRepository, times(1)).save(any());
  }
}
