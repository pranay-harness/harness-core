/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.expressions;

import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;

import com.google.common.collect.ImmutableList;
import java.security.SecureRandom;
import java.util.List;
import org.apache.commons.codec.binary.Base32;
import org.hibernate.validator.constraints.NotEmpty;

public class SampleExpressionEvaluator extends EngineExpressionEvaluator {
  private final boolean supportStringUtils;

  public SampleExpressionEvaluator(VariableResolverTracker variableResolverTracker, boolean supportStringUtils) {
    super(variableResolverTracker);
    this.supportStringUtils = supportStringUtils;
  }

  @Override
  protected void initialize() {
    super.initialize();
    if (supportStringUtils) {
      // If supportStringUtils is true, support expressions like ${stringUtils.toUpper("abc")} and
      // ${stringUtils.toLower("ABC")}.
      addToContext("stringUtils", new StringUtilsFunctor());

      // Add an alias of string -> stringUtils. Support expressions like ${string.toUpper("abc")} and
      // ${string.toLower("ABC")}.
      addStaticAlias("string", "stringUtils");
    }

    // Support expressions like ${random.generateRandom()}
    addToContext("random", new RandomFunctor());
  }

  @Override
  @NotEmpty
  protected List<String> fetchPrefixes() {
    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    if (supportStringUtils) {
      // Adding stringUtils makes it possible to use ${toUpper("abc")} instead of ${stringUtils.toUpper("abc")}. Only
      // add the stringUtils prefix if supportStringUtils is true.
      listBuilder.add("stringUtils");
    }

    // Adding random makes it possible to use ${generateRandom()} instead of ${random.generateRandom()}.
    listBuilder.add("random");

    // Add all the prefixes of the superclass.
    return listBuilder.addAll(super.fetchPrefixes()).build();
  }

  public static class StringUtilsFunctor {
    public String toUpper(String str) {
      return str == null ? "" : str.toUpperCase();
    }

    public String toLower(String str) {
      return str == null ? "" : str.toLowerCase();
    }
  }

  public static class RandomFunctor {
    private static final Base32 base32 = new Base32();
    private static final SecureRandom random = new SecureRandom();

    public String generateRandom(String str) {
      byte[] bytes = new byte[16];
      random.nextBytes(bytes);
      return base32.encodeAsString(bytes);
    }
  }
}
