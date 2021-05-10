package io.harness.accesscontrol.preference;

import io.harness.accesscontrol.preference.daos.AccessControlPreferenceDAO;
import io.harness.accesscontrol.preference.daos.AccessControlPreferenceDAOImpl;
import io.harness.accesscontrol.preference.daos.NoOpAccessControlPreferenceDAOImpl;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceServiceImpl;
import io.harness.accesscontrol.preference.services.NoOpAccessControlPreferenceServiceImpl;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.PL)
public class AccessControlPreferenceModule extends AbstractModule {
  private static AccessControlPreferenceModule instance;
  private final AccessControlPreferenceConfiguration configuration;

  private AccessControlPreferenceModule(AccessControlPreferenceConfiguration configuration) {
    this.configuration = configuration;
  }

  public static synchronized AccessControlPreferenceModule getInstance(
      AccessControlPreferenceConfiguration accessControlPreferenceConfiguration) {
    if (instance == null) {
      instance = new AccessControlPreferenceModule(accessControlPreferenceConfiguration);
    }
    return instance;
  }

  @Override
  protected void configure() {
    if (configuration.isEnabled()) {
      bind(AccessControlPreferenceService.class).to(AccessControlPreferenceServiceImpl.class).in(Scopes.SINGLETON);
      bind(AccessControlPreferenceDAO.class).to(AccessControlPreferenceDAOImpl.class).in(Scopes.SINGLETON);
    } else {
      bind(AccessControlPreferenceService.class).to(NoOpAccessControlPreferenceServiceImpl.class).in(Scopes.SINGLETON);
      bind(AccessControlPreferenceDAO.class).to(NoOpAccessControlPreferenceDAOImpl.class).in(Scopes.SINGLETON);
    }
  }
}
