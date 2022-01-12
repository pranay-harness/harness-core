/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.loginSettings;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.passay.CharacterData;

@OwnedBy(HarnessTeam.PL)
public enum SpecialCharactersPasswordData implements CharacterData {
  SpecialCharactersAllowedInPassword("INSUFFICIENT_SPECIAL_CHARECTERS", "~!@#$%^&*_-+=`|\\(){}[]:;\"'<>,.?/");

  private final String errorCode;
  private final String characters;

  SpecialCharactersPasswordData(String errorCode, String characters) {
    this.errorCode = errorCode;
    this.characters = characters;
  }

  @Override
  public String getErrorCode() {
    return this.errorCode;
  }

  @Override
  public String getCharacters() {
    return characters;
  }
}
