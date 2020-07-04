package software.wings.service.impl;

import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
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
  @Owner(developers = YOGESH)
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

    assertThat(gitConfig.getBranch()).isEqualTo("master");
    assertThat(gitConfig.getRepoUrl()).isEqualTo("github.com");
    assertThat(gitConfig.getReference()).isEqualTo("tag-1");
  }
}