/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.preference;

import io.harness.accesscontrol.preference.persistence.daos.AccessControlPreferenceDAO;
import io.harness.accesscontrol.preference.persistence.daos.AccessControlPreferenceDAOImpl;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceServiceImpl;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

@OwnedBy(HarnessTeam.PL)
public class AccessControlPreferenceModule extends AbstractModule {
  private static AccessControlPreferenceModule instance;

  private AccessControlPreferenceModule() {}

  public static synchronized AccessControlPreferenceModule getInstance() {
    if (instance == null) {
      instance = new AccessControlPreferenceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(AccessControlPreferenceService.class).to(AccessControlPreferenceServiceImpl.class).in(Scopes.SINGLETON);
    bind(AccessControlPreferenceDAO.class).to(AccessControlPreferenceDAOImpl.class).in(Scopes.SINGLETON);
  }
}
