/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
