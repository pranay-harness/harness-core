/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.ldap;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

/**
 * @author deepakPatankar on 11/27/19
 */
@OwnedBy(PL)
public class LdapDelegateException extends WingsException {
  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public LdapDelegateException(String message, Throwable throwable) {
    super(message, throwable, GENERAL_ERROR, Level.ERROR, null, null);
  }
}
