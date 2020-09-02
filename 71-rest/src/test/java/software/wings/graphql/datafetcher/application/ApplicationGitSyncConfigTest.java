package software.wings.graphql.datafetcher.application;

import static io.harness.rule.OwnerRule.HINGER;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.application.input.QLUpdateApplicationGitSyncConfigInput;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;

public class ApplicationGitSyncConfigTest extends CategoryTest {
  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationUpdateGitSyncConfig() throws NoSuchMethodException {
    Method method = UpdateApplicationGitSyncConfigDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_APPLICATIONS);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationUpdateGitSyncConfigStatus() throws NoSuchMethodException {
    Method method = UpdateApplicationGitSyncConfigStatusDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_APPLICATIONS);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForApplicationRemoveGitSync() throws NoSuchMethodException {
    Method method = RemoveApplicationGitSyncConfigDataFetcher.class.getDeclaredMethod(
        "mutateAndFetch", QLUpdateApplicationGitSyncConfigInput.class, MutationContext.class);
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_APPLICATIONS);
  }
}
