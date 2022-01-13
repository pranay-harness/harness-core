/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.interfaces.clients.local;

import io.harness.exception.UnsupportedOperationException;
import io.harness.licensing.Edition;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.interfaces.clients.ModuleLicenseClient;

public class UnsupportedClient implements ModuleLicenseClient {
  @Override
  public ModuleLicenseDTO createTrialLicense(Edition edition, String accountId) {
    throw new UnsupportedOperationException("Requested module type hasn't been supported");
  }
}
