package software.wings.service.intfc;

import software.wings.beans.AuthToken;
import software.wings.beans.User;
import software.wings.dl.PageRequest.PageRequestType;
import software.wings.security.PermissionAttribute;

import java.util.List;

/**
 * Created by peeyushaggarwal on 8/18/16.
 */
public interface AuthService {
  /**
   * Validate token auth token.
   *
   * @param tokenString the token string
   * @return the auth token
   */
  AuthToken validateToken(String tokenString);

  /**
   * Authorize.
   *
   * @param appId                the app id
   * @param envId                the env id
   * @param user                 the user
   * @param permissionAttributes the permission attributes
   * @param requestType          the request type
   */
  void authorize(String appId, String envId, User user, List<PermissionAttribute> permissionAttributes,
      PageRequestType requestType);

  void validateDelegateToken(String accountId, String tokenString);
}
