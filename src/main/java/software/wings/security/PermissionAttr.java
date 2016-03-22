package software.wings.security;

import static software.wings.security.PermissionAttr.Action.*;
import static software.wings.security.PermissionAttr.Resource.*;

/**
 * Created by anubhaw on 3/10/16.
 */

public enum PermissionAttr {
  APP_CREATE(APP, CREATE),
  APP_READ(APP, READ, false, false),
  APP_DELETE(APP, DELETE),
  PLATFORM_CREATE(PLATFORM, CREATE),
  PLATFORM_READ(PLATFORM, READ, false, false),
  PLATFORM_DELETE(PLATFORM, DELETE),
  SERVICE(ANY, ALL),
  CONFIG(ANY, ALL),
  ENV(ANY, ALL),
  ROLE(ANY, ALL),
  USER(ANY, ALL),
  DEPLOYMENT(ANY, ALL),
  RELEASE(ANY, ALL),
  DELIVERY(ANY, ALL),
  ARTIFACTS(ANY, ALL);

  public enum Resource {
    ANY,
    APP,
    PLATFORM,
    SERVICE,
    CONFIG,
    ENV,
    ROLE,
    HOST,
    DEPLOYMENT,
    RELEASE,
    DELIVERY,
    ARTIFCATS,
    USER;
  }
  public enum Action {
    ALL,
    CREATE,
    READ,
    DELETE;
  }

  PermissionAttr(Resource resource, Action action) {
    this.resource = resource;
    this.action = action;
  }

  PermissionAttr(Resource resource, Action action, boolean onApp, boolean onEnv) {
    this.resource = resource;
    this.action = action;
    this.onApp = onApp;
    this.onEnv = onEnv;
  }

  private Resource resource;
  private Action action;
  private boolean onApp = true;
  private boolean onEnv = true;

  public Resource getResource() {
    return resource;
  }
  public Action getAction() {
    return action;
  }
  public boolean isOnApp() {
    return onApp;
  }
  public boolean isOnEnv() {
    return onEnv;
  }
}