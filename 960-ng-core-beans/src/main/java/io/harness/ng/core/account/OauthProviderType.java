/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
