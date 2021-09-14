/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OwnedBy(CDC)
public class RegexFunctor implements ExpressionFunctor {
  public String extract(String pattern, String source) {
    if (source == null) {
      return "";
    }
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    while (matcher.find()) {
      String match = matcher.group();
      if (!match.isEmpty()) {
        return match;
      }
    }
    return "";
  }

  public String replace(String pattern, String replacement, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    return matcher.replaceAll(replacement);
  }

  public boolean match(String pattern, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    return matcher.find();
  }
}
