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
