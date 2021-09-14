/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.deployment;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A pre-deployment checker is a check that is done before a deployment to decide is a deployment should go through or
 * not.
 *
 * For example: {@link software.wings.service.impl.deployment.checks.AccountExpirationChecker} checks whether an account
 * is expired or not. If the account is expired, it doesn't let the deployment go through
 */
@ParametersAreNonnullByDefault
public interface PreDeploymentChecker {
  void check(String accountId);
}
