package software.wings.beans.loginSettings;

import static software.wings.beans.loginSettings.SpecialCharactersPasswordData.SpecialCharactersAllowedInPassword;

import static org.passay.EnglishCharacterData.Alphabetical;
import static org.passay.EnglishCharacterData.Digit;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.passay.CharacterData;

@OwnedBy(HarnessTeam.PL)
public enum AllowedCharactersPasswordData implements CharacterData {
  AllCharactersPasswordData("INSUFFICIENT_CHARACTERS",
      SpecialCharactersAllowedInPassword.getCharacters() + Alphabetical.getCharacters() + Digit.getCharacters());

  private final String errorCode;
  private final String characters;

  AllowedCharactersPasswordData(String errorCode, String characters) {
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
