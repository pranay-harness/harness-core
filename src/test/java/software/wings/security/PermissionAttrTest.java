package software.wings.security;

import org.junit.Test;

import static software.wings.security.PermissionAttr.Action.CREATE;

/**
 * Created by anubhaw on 3/18/16.
 */
public class PermissionAttrTest {
  @Test
  public void testHelper() {
    PermissionAttr ra = APP(CREATE, false, false);
  }
}