/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.provisioners.utils;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;

import com.google.inject.Singleton;

@Singleton
public class InfraProvisionerUtils {
  private static final String secretKeyName = "aws_playground_secret_key";

  public static InfrastructureProvisioner setValuesToProvisioner(
      InfrastructureProvisioner provisioner, ScmSecret scmSecret, String secretKeyValue) throws Exception {
    for (NameValuePair variable : provisioner.getVariables()) {
      String value;
      switch (variable.getName()) {
        case "access_key":
          value = String.valueOf(scmSecret.decryptToString(SecretName.builder().value(secretKeyName).build()));
          break;
        case "secret_key":
          value = secretKeyValue;
          break;
        default:
          throw new Exception("Unknown variable: " + variable.getName() + " in provisioner");
      }
      variable.setValue(value);
    }

    return provisioner;
  }
}
