package io.harness.accesscontrol;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.permissions.PermissionsModule;
import io.harness.accesscontrol.principals.PrincipalModule;
import io.harness.accesscontrol.resources.ResourceModule;
import io.harness.accesscontrol.roleassignments.RoleAssignmentModule;
import io.harness.accesscontrol.roles.RoleModule;
import io.harness.accesscontrol.scopes.ScopeModule;
import io.harness.accesscontrol.scopes.core.ScopeLevel;
import io.harness.accesscontrol.scopes.core.ScopeParamsFactory;
import io.harness.annotations.dev.OwnedBy;
import io.harness.outbox.api.OutboxService;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Map;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
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
    install(ResourceModule.getInstance());
    install(ScopeModule.getInstance());
    install(PermissionsModule.getInstance());
    install(RoleModule.getInstance());
    install(PrincipalModule.getInstance());
    install(RoleAssignmentModule.getInstance());
    registerRequiredBindings();
  }

  private void registerRequiredBindings() {
    requireBinding(OutboxService.class);
    requireBinding(TransactionTemplate.class);
    requireBinding(MongoTemplate.class);
    requireBinding(ScopeParamsFactory.class);
    requireBinding(Key.get(new TypeLiteral<Map<String, ScopeLevel>>() {}));
  }
}
