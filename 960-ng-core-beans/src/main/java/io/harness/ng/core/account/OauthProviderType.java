/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * @author marklu on 2019-05-11
 */
@OwnedBy(HarnessTeam.PL)
public enum OauthProviderType {
  AZURE,
  BITBUCKET,
  GITHUB,
  GITLAB,
  GOOGLE,
  LINKEDIN;
}
