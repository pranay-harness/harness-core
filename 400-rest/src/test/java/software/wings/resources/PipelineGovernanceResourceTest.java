package software.wings.resources;

import static io.harness.rule.OwnerRule.HINGER;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_PIPELINE_GOVERNANCE_STANDARDS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.rule.Owner;

import software.wings.resources.governance.PipelineGovernanceResource;
import software.wings.security.annotations.AuthRule;

import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class PipelineGovernanceResourceTest extends CategoryTest {
  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForAdd() throws NoSuchMethodException {
    Method method =
        PipelineGovernanceResource.class.getDeclaredMethod("add", String.class, PipelineGovernanceConfig.class);
    AuthRule authRule = method.getAnnotation(AuthRule.class);
    assertThat(authRule.permissionType()).isEqualTo(MANAGE_PIPELINE_GOVERNANCE_STANDARDS);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForUpdate() throws NoSuchMethodException {
    Method method = PipelineGovernanceResource.class.getDeclaredMethod("update", String.class, String.class);
    AuthRule authRule = method.getAnnotation(AuthRule.class);
    assertThat(authRule.permissionType()).isEqualTo(MANAGE_PIPELINE_GOVERNANCE_STANDARDS);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrectForList() throws NoSuchMethodException {
    Method method = PipelineGovernanceResource.class.getDeclaredMethod("list", String.class);
    AuthRule authRule = method.getAnnotation(AuthRule.class);
    assertThat(authRule.permissionType()).isEqualTo(MANAGE_PIPELINE_GOVERNANCE_STANDARDS);
  }
}
