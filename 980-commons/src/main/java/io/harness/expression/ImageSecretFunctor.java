/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.expression;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.InvalidRequestException;

@OwnedBy(HarnessTeam.CDC)
public class ImageSecretFunctor implements ExpressionFunctor {
  public static final String FUNCTOR_NAME = "imageSecret";
  private static final String REGISTRY_CREDENTIAL_TEMPLATE = "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  public String create(String registryUrl, String userName, String password) {
    if (ExpressionEvaluator.matchesVariablePattern(registryUrl) || ExpressionEvaluator.matchesVariablePattern(userName)
        || ExpressionEvaluator.matchesVariablePattern(password)) {
      throw new InvalidRequestException("Arguments cannot be expression");
    }
    return EncodingUtils.encodeBase64(format(
        REGISTRY_CREDENTIAL_TEMPLATE, registryUrl, userName, password.replaceAll("\n", "").replaceAll("\"", "\\\\\"")));
  }
}
