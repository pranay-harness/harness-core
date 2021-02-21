package io.harness.accesscontrol;

import io.harness.accesscontrol.permissions.PermissionsModule;
import io.harness.accesscontrol.roleassignments.RoleAssignmentModule;
import io.harness.accesscontrol.roles.RoleModule;
import io.harness.accesscontrol.scopes.ScopeModule;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public class AccessControlCoreModule extends AbstractModule {
  private static AccessControlCoreModule instance;

  private AccessControlCoreModule() {}

  public static synchronized AccessControlCoreModule getInstance() {
    if (instance == null) {
      instance = new AccessControlCoreModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(ScopeModule.getInstance());
    install(PermissionsModule.getInstance());
    install(RoleModule.getInstance());
    install(RoleAssignmentModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(TransactionTemplate.class);
    requireBinding(MongoTemplate.class);
    requireBinding(ScopeParamsFactory.class);
    requireBinding(Key.get(new TypeLiteral<Map<String, ScopeLevel>>() {}));
  }
}
