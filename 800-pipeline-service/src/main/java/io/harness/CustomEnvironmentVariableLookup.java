package io.harness;

import io.harness.data.structure.EmptyPredicate;

import io.dropwizard.configuration.UndefinedEnvironmentVariableException;
import org.apache.commons.text.StrLookup;

public class CustomEnvironmentVariableLookup extends StrLookup<Object> {
  private final boolean strict;

  public CustomEnvironmentVariableLookup() {
    this(true);
  }

  public CustomEnvironmentVariableLookup(boolean strict) {
    this.strict = strict;
  }

  public String lookup(String key) {
    String value = System.getenv(key);
    if (EmptyPredicate.isEmpty(value) && this.strict) {
      throw new UndefinedEnvironmentVariableException("The environment variable '" + key
          + "' is not defined; could not substitute the expression '${" + key + "}'.");
    } else {
      return value;
    }
  }
}