package software.wings.service.impl;

import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.sm.ExecutionContext;

public class GitConfigHelperServiceTest extends WingsBaseTest {
  @Mock ExecutionContext context;

  @Inject private GitConfigHelperService gitConfigHelperService;

  @Test
  @Owner(developers = YOGESH_CHAUHAN)
  @Category(UnitTests.class)
  public void testRenderGitConfig() {
    String branchExpression = "${branch}";
    String urlExpression = "${url}";
    String refExpression = "${ref}";
    GitConfig gitConfig = GitConfig.builder().branch(branchExpression).repoUrl(urlExpression).build();
    gitConfig.setReference(refExpression);

    when(context.renderExpression(branchExpression)).thenReturn("master");
    when(context.renderExpression(urlExpression)).thenReturn("github.com");
    when(context.renderExpression(refExpression)).thenReturn("tag-1");

    gitConfigHelperService.renderGitConfig(context, gitConfig);

    Assertions.assertThat(gitConfig.getBranch()).isEqualTo("master");
    Assertions.assertThat(gitConfig.getRepoUrl()).isEqualTo("github.com");
    Assertions.assertThat(gitConfig.getReference()).isEqualTo("tag-1");
  }
}