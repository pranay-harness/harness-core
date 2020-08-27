package software.wings.security;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.graphql.schema.type.permissions.QLActions;

/**
 * This test is to make sure QL Permissions/Actions
 * are in sync with actual Permissions/Actions.
 *
 * created by ujjawal
 */

public class PermissionAttributeTest extends WingsBaseTest {
  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAddedPermissionsAreInSync() {
    assertThat(PermissionAttribute.PermissionType.values().length).isEqualTo(39);
    assertThat(QLAccountPermissionType.values().length).isEqualTo(25);
    assertThat(PermissionAttribute.PermissionType.values().length - 39)
        .isEqualTo(QLAccountPermissionType.values().length - 25);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testAddedActionsAreInSync() {
    assertThat(PermissionAttribute.Action.values().length).isEqualTo(9);
    assertThat(QLActions.values().length).isEqualTo(7);
    assertThat(PermissionAttribute.Action.values().length - 9).isEqualTo(QLActions.values().length - 7);
  }
}
