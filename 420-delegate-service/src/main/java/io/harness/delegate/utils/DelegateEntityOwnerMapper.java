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
public class DelegateEntityOwnerMapper {
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
}
