package io.harness.data.parser;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class CsvParserTest {
  @Test
  @Category(UnitTests.class)
  public void testCsvParseCommaSeparatedString() {
    {
      String input = "A,B,C,D";
      List<String> output = CsvParser.parse(input);
      assertEquals(output.size(), 4);
      assertEquals(output.get(0), "A");
      assertEquals(output.get(1), "B");
      assertEquals(output.get(2), "C");
      assertEquals(output.get(3), "D");
    }

    {
      String input = "A,B,Hello\\,World, D";
      List<String> output = CsvParser.parse(input);
      assertEquals(output.size(), 4);
      assertEquals(output.get(0), "A");
      assertEquals(output.get(1), "B");
      assertEquals(output.get(2), "Hello,World");
      assertEquals(output.get(3), "D");
    }
  }
}