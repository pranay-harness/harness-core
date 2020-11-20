package io.harness.gitsync.core.service;

import static io.harness.rule.OwnerRule.ABHINAV;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.dao.api.repositories.GitCommit.GitCommitRepository;
import io.harness.gitsync.core.impl.GitCommitServiceImpl;
import io.harness.rule.Owner;
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
    gitCommitService.save(GitCommit.builder().build());
    verify(gitCommitRepository, times(1)).save(any());
  }
}
