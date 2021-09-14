/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateEntityOwner;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.DEL)
public class DelegateEntityOwnerHelper {
  private static final String SEPARATOR = "/";

  public static DelegateEntityOwner buildOwner(String orgIdentifier, String projectIdentifier) {
    // Account level object
    if (isBlank(orgIdentifier) && isBlank(projectIdentifier)) {
      return null;
    }

    StringBuilder ownerIdentifier = new StringBuilder();
    ownerIdentifier.append(isNotBlank(orgIdentifier) ? orgIdentifier : EMPTY)
        .append(isNotBlank(ownerIdentifier) && isNotBlank(projectIdentifier) ? SEPARATOR : EMPTY)
        .append(isNotBlank(projectIdentifier) ? projectIdentifier : EMPTY);

    return DelegateEntityOwner.builder().identifier(ownerIdentifier.toString()).build();
  }

  public static String extractOrgIdFromOwnerIdentifier(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }

    String[] identifierElements = identifier.split("/");

    return identifierElements.length >= 1 ? identifierElements[0] : null;
  }

  public static String extractProjectIdFromOwnerIdentifier(String identifier) {
    if (isBlank(identifier)) {
      return null;
    }

    String[] identifierElements = identifier.split("/");

    return identifierElements.length >= 2 ? identifierElements[1] : null;
  }

  public static boolean isAccount(final DelegateEntityOwner owner) {
    return owner == null
        || (extractOrgIdFromOwnerIdentifier(owner.getIdentifier()) == null
            && extractProjectIdFromOwnerIdentifier(owner.getIdentifier()) == null);
  }

  public static boolean isOrganisation(final DelegateEntityOwner owner) {
    return owner != null && extractOrgIdFromOwnerIdentifier(owner.getIdentifier()) != null
        && extractProjectIdFromOwnerIdentifier(owner.getIdentifier()) == null;
  }

  public static boolean isProject(final DelegateEntityOwner owner) {
    return owner != null && extractProjectIdFromOwnerIdentifier(owner.getIdentifier()) != null;
  }
}
