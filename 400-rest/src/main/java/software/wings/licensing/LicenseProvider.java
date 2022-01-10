/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.licensing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.License;

import java.util.List;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
@OwnedBy(HarnessTeam.GTM)
@TargetModule(HarnessModule._945_ACCOUNT_MGMT)
public interface LicenseProvider {
  List<License> getActiveLicenses();
  License get(String licenseId);
}
