package software.wings.service.intfc;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.security.PermissionAttr;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
public interface AuthService {
  AuthToken validateToken(String tokenString);

  void authorize(String appId, String envId, User user, List<PermissionAttr> permissionAttrs);
}
