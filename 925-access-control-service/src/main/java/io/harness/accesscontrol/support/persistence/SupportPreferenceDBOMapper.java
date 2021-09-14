/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.support.persistence;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.support.SupportPreference;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class SupportPreferenceDBOMapper {
  public static SupportPreferenceDBO toDBO(SupportPreference object) {
    return SupportPreferenceDBO.builder()
        .accountIdentifier(object.getAccountIdentifier())
        .isSupportEnabled(object.isSupportEnabled())
        .build();
  }

  public static SupportPreference fromDBO(SupportPreferenceDBO object) {
    return SupportPreference.builder()
        .accountIdentifier(object.getAccountIdentifier())
        .isSupportEnabled(object.isSupportEnabled())
        .build();
  }
}
