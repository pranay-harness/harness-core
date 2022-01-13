/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.loginSettings;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;

import io.harness.annotations.dev.TargetModule;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordData;
import org.passay.PasswordValidator;

@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public enum PasswordStrengthChecks {
  MUST_HAVE_UPPERCASE_LETTERS() {
    @Override
    public boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy) {
      int minNumberOfUppercaseCharacters = passwordStrengthPolicy.getMinNumberOfUppercaseCharacters();
      if (minNumberOfUppercaseCharacters == 0) {
        return true;
      }
      PasswordValidator passwordMatcher =
          new PasswordValidator(new CharacterRule(EnglishCharacterData.UpperCase, minNumberOfUppercaseCharacters));
      return passwordMatcher.validate(passwordData).isValid();
    }

    @Override
    public int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy) {
      return passwordStrengthPolicy.getMinNumberOfUppercaseCharacters();
    }
  },

  MUST_HAVE_LOWERCASE_LETTERS() {
    @Override
    public boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy) {
      int minNumberOfLowercaseLetters = passwordStrengthPolicy.getMinNumberOfLowercaseCharacters();

      // If the value is 0, it means that the strength rule is disabled.
      if (minNumberOfLowercaseLetters == 0) {
        return true;
      }
      PasswordValidator passwordMatcher =
          new PasswordValidator(new CharacterRule(EnglishCharacterData.LowerCase, minNumberOfLowercaseLetters));
      return passwordMatcher.validate(passwordData).isValid();
    }

    @Override
    public int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy) {
      return passwordStrengthPolicy.getMinNumberOfLowercaseCharacters();
    }
  },

  MUST_HAVE_DIGITS() {
    @Override
    public boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy) {
      int minNumberOfDigits = passwordStrengthPolicy.getMinNumberOfDigits();

      // If the value is 0, it means that the strength rule is disabled.
      if (minNumberOfDigits == 0) {
        return true;
      }
      PasswordValidator passwordMatcher =
          new PasswordValidator(new CharacterRule(EnglishCharacterData.Digit, minNumberOfDigits));
      return passwordMatcher.validate(passwordData).isValid();
    }

    @Override
    public int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy) {
      return passwordStrengthPolicy.getMinNumberOfDigits();
    }
  },

  MUST_HAVE_SPECIAL_CHARACTERS() {
    @Override
    public boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy) {
      int minNumberOfSpecialCharacters = passwordStrengthPolicy.getMinNumberOfSpecialCharacters();

      // If the value is 0, it means that the strength rule is disabled.
      if (minNumberOfSpecialCharacters == 0) {
        return true;
      }
      PasswordValidator passwordMatcher = new PasswordValidator(new CharacterRule(
          SpecialCharactersPasswordData.SpecialCharactersAllowedInPassword, minNumberOfSpecialCharacters));
      return passwordMatcher.validate(passwordData).isValid();
    }

    @Override
    public int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy) {
      return passwordStrengthPolicy.getMinNumberOfSpecialCharacters();
    }
  },

  MINIMUM_NUMBER_OF_CHARACTERS() {
    @Override
    public boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy) {
      int minNumberOfCharacters = passwordStrengthPolicy.getMinNumberOfCharacters();

      // If the value is 0, it means that the strength rule is disabled.
      if (minNumberOfCharacters == 0) {
        return true;
      }
      PasswordValidator passwordMatcher = new PasswordValidator(
          new CharacterRule(AllowedCharactersPasswordData.AllCharactersPasswordData, minNumberOfCharacters));
      return passwordMatcher.validate(passwordData).isValid();
    }

    @Override
    public int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy) {
      return passwordStrengthPolicy.getMinNumberOfCharacters();
    }
  };

  public abstract boolean validate(PasswordData passwordData, PasswordStrengthPolicy passwordStrengthPolicy);

  public abstract int getMinimumCount(PasswordStrengthPolicy passwordStrengthPolicy);
}
