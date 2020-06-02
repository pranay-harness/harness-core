package io.harness.data.validator;

import static io.harness.rule.OwnerRule.ANKIT;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Random;
import java.util.regex.Pattern;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class EntityIdentifierValidatorTest {
  private static final Pattern ALLOWED_CHARS_PATTERN = Pattern.compile("[a-zA-Z0-9-_]+");

  @Builder
  static class EntityIdentifierValidatorTestStructure {
    @EntityIdentifier String identifier;
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testEntityIdentifierValidator() {
    assertFalse(EntityIdentifierValidator.isValidEntityIdentifier(null));

    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    for (int i = 0; i < 5000; i++) {
      String identifier = generateRandomAsciiString();
      int violationsCount =
          validator.validate(EntityIdentifierValidatorTestStructure.builder().identifier(identifier).build()).size();
      if (isValidEntityIdentifier(identifier)) {
        assertEquals("identifier : " + identifier, 0, violationsCount);
      } else {
        assertTrue("identifier : " + identifier, violationsCount > 0);
      }
    }
  }

  private static String generateRandomAsciiString() {
    String random = RandomStringUtils.randomAscii(100);
    return random.substring(0, new Random().nextInt(100));
  }

  private static boolean isValidEntityIdentifier(String identifier) {
    if (identifier == null || identifier.length() < 3 || identifier.length() > 64) {
      return false;
    }

    char startChar = identifier.charAt(0);
    char endChar = identifier.charAt(identifier.length() - 1);
    boolean hasAllowedChars = ALLOWED_CHARS_PATTERN.matcher(identifier).matches();

    return hasAllowedChars && isLetterOrDigit(startChar) && isLetterOrDigit(endChar);
  }

  private static boolean isLetterOrDigit(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
  }
}