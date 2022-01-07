/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.licensing;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.License;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
@OwnedBy(HarnessTeam.GTM)
@TargetModule(HarnessModule._945_ACCOUNT_MGMT)
public class DatabaseLicenseProviderImpl implements LicenseProvider {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<License> getActiveLicenses() {
    return wingsPersistence.createQuery(License.class, excludeAuthority).asList();
  }

  @Override
  public License get(String licenseId) {
    return wingsPersistence.get(License.class, licenseId);
  }
}
