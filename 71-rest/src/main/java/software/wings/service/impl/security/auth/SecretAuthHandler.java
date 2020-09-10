package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import software.wings.security.PermissionAttribute;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(PL)
public class SecretAuthHandler {
  @Inject private AuthHandler authHandler;

  public void authorize() {
    List<PermissionAttribute> permissionAttributeList = new ArrayList<>();
    permissionAttributeList.add(new PermissionAttribute(MANAGE_SECRETS));
    authHandler.authorizeAccountPermission(permissionAttributeList);
  }
}
