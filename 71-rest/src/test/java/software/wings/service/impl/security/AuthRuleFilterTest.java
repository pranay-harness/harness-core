package software.wings.service.impl.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.security.AuthRuleFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;

import java.util.Arrays;

public class AuthRuleFilterTest extends WingsBaseTest {
  @Inject AuthRuleFilter authRuleFilter;
  @Test
  @Category(UnitTests.class)
  public void testIsAccountLevelPermission() {
    PermissionAttribute permissionAttribute = new PermissionAttribute(PermissionType.AUDIT_VIEWER, Action.READ);
    PermissionAttribute permissionAttribute1 = new PermissionAttribute(PermissionType.APP, Action.ALL);
    assertTrue(authRuleFilter.isAccountLevelPermissions(Arrays.asList(permissionAttribute, permissionAttribute1)));

    permissionAttribute = new PermissionAttribute(PermissionType.APP, Action.READ);
    assertFalse(authRuleFilter.isAccountLevelPermissions(Arrays.asList(permissionAttribute)));
  }
}
