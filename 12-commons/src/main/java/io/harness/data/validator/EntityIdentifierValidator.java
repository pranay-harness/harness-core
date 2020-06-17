package io.harness.data.validator;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class EntityIdentifierValidator implements ConstraintValidator<EntityIdentifier, String> {
  // Max Length : 64 characters
  // Start with: Alphabets, characters, Underscore  or $
  // Chars Allowed : Alphanumeric, Underscore, $
  public static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_$][0-9a-zA-Z_$]{1,63}$");
  public static final Set<String> NOT_ALLOWED_WORDS =
      Stream
          .of("or", "and", "eq", "ne", "lt", "gt", "le", "ge", "div", "mod", "not", "null", "true", "false", "new",
              "var", "return")
          .collect(Collectors.toCollection(HashSet::new));

  @Override
  public void initialize(EntityIdentifier constraintAnnotation) {
    // Nothing to initialize
  }

  @Override
  public boolean isValid(String identifier, ConstraintValidatorContext context) {
    return isNotBlank(identifier) && matchesIdentifierPattern(identifier) && hasAllowedWords(identifier);
  }

  @VisibleForTesting
  boolean matchesIdentifierPattern(String identifier) {
    return IDENTIFIER_PATTERN.matcher(identifier).matches();
  }

  @VisibleForTesting
  boolean hasAllowedWords(String identifier) {
    return identifier != null && !NOT_ALLOWED_WORDS.contains(identifier);
  }
}