/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.utils;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;
import org.apache.http.util.Asserts;

@UtilityClass
public class FullyQualifiedIdentifierHelper {
  public String getFullyQualifiedIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    if (EmptyPredicate.isEmpty(identifier)) {
      throw new InvalidRequestException("Invalid Request Exception: No identifier provided.");
    }
    if (EmptyPredicate.isNotEmpty(projectIdentifier)) {
      Asserts.notEmpty(orgIdentifier, "Org Identifier");
      Asserts.notEmpty(accountId, "Account Identifier");
      return String.format("%s/%s/%s/%s", accountId, orgIdentifier, projectIdentifier, identifier);
    } else if (EmptyPredicate.isNotEmpty(orgIdentifier)) {
      Asserts.notEmpty(accountId, "Account Identifier");
      return String.format("%s/%s/%s", accountId, orgIdentifier, identifier);
    } else if (EmptyPredicate.isNotEmpty(accountId)) {
      return String.format("%s/%s", accountId, identifier);
    }
    throw new InvalidRequestException("Invalid Request Exception: No account ID provided.");
  }
}
