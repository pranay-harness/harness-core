package io.harness.data.validator;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.data.validator.TagValidator.TAG_MAX_LENGTH;
import static io.harness.rule.OwnerRule.ANKIT;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Random;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import lombok.Builder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TagValidatorTest {
  @Builder
  static class TagValidatorTestStructure {
    @Tag String identifier;
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testTagValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();

    assertFalse(TagValidator.isValidTag(null));
    assertFalse(TagValidator.isValidTag(""));

    for (int i = 0; i < 1000; i++) {
      String tag = generateRandomString();
      int violationsCount = validator.validate(TagValidatorTestStructure.builder().identifier(tag).build()).size();
      if (isValidTag(tag)) {
        assertEquals("tag : " + tag, 0, violationsCount);
      } else {
        assertTrue("tag : " + tag, violationsCount > 0);
      }
    }
  }

  private static String generateRandomString() {
    String random = RandomStringUtils.random(500);
    return random.substring(0, new Random().nextInt(500));
  }

  private static boolean isValidTag(String tag) {
    return hasSome(tag) && tag.length() <= TAG_MAX_LENGTH;
  }
}
