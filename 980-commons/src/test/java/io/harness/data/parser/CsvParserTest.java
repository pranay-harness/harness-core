/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.parser;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CsvParserTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testCsvParseCommaSeparatedString() {
    {
      String input = "A,B,C,D";
      List<String> output = CsvParser.parse(input);
      assertThat(4).isEqualTo(output.size());
      assertThat("A").isEqualTo(output.get(0));
      assertThat("B").isEqualTo(output.get(1));
      assertThat("C").isEqualTo(output.get(2));
      assertThat("D").isEqualTo(output.get(3));
    }

    {
      String input = "A,B,Hello\\,World, D";
      List<String> output = CsvParser.parse(input);
      assertThat(4).isEqualTo(output.size());
      assertThat("A").isEqualTo(output.get(0));
      assertThat("B").isEqualTo(output.get(1));
      assertThat(output.get(2)).isEqualTo("Hello,World");
      assertThat("D").isEqualTo(output.get(3));
    }
  }
}
