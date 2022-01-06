/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
