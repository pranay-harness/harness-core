package io.harness;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.graphql.scalar.GraphQLDateTimeScalar.INVALID_AST_TYPE;
import static software.wings.graphql.scalar.GraphQLDateTimeScalar.INVALID_INPUT_INSTANCE_TYPE;

import graphql.language.StringValue;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingSerializeException;
import io.harness.category.element.UnitTests;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import software.wings.WingsBaseTest;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;

import java.time.OffsetDateTime;

public class GraphQLDateTimeScalarTest extends WingsBaseTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Category(UnitTests.class)
  public void testParseLiteralWithInvalidInput() {
    Integer invalidInput = 1;
    thrown.expect(CoercingParseLiteralException.class);
    thrown.expectMessage(INVALID_AST_TYPE + GraphQLDateTimeScalar.typeName(invalidInput));
    GraphQLDateTimeScalar.type.getCoercing().parseLiteral(invalidInput);
  }

  @Test
  @Category(UnitTests.class)
  public void testParseLiteral() {
    long currentTimeInMillis = System.currentTimeMillis();
    String dateAsString = GraphQLDateTimeScalar.convertToString(currentTimeInMillis);
    OffsetDateTime expectedDateTime = GraphQLDateTimeScalar.parseOffsetDateTime(dateAsString, null);
    StringValue dateTime = new StringValue(dateAsString);
    OffsetDateTime actualOffsetDateTime =
        (OffsetDateTime) GraphQLDateTimeScalar.type.getCoercing().parseLiteral(dateTime);
    assertThat(actualOffsetDateTime.toInstant().toEpochMilli()).isEqualTo(expectedDateTime.toInstant().toEpochMilli());
    assertThat(actualOffsetDateTime.toString()).isEqualTo(expectedDateTime.toString());
  }

  @Test
  @Category(UnitTests.class)
  public void testSerializeWithInvalidInput() {
    Integer invalidInput = 1;
    thrown.expect(CoercingSerializeException.class);
    thrown.expectMessage(INVALID_INPUT_INSTANCE_TYPE + GraphQLDateTimeScalar.typeName(invalidInput));
    GraphQLDateTimeScalar.type.getCoercing().serialize(invalidInput);
  }

  @Test
  @Category(UnitTests.class)
  public void testSerialize() {
    long currentTimeInMillis = System.currentTimeMillis();
    OffsetDateTime offsetDateTime = GraphQLDateTimeScalar.convert(currentTimeInMillis);
    String expectedFormattedDateTime = GraphQLDateTimeScalar.formatDateTime(offsetDateTime);
    String actualFormattedDateTime = (String) GraphQLDateTimeScalar.type.getCoercing().serialize(offsetDateTime);
    assertThat(expectedFormattedDateTime).isEqualTo(actualFormattedDateTime);
  }

  @Test
  @Category(UnitTests.class)
  public void testSerializeWithString() {
    long currentTimeInMillis = System.currentTimeMillis();
    String dateAsString = GraphQLDateTimeScalar.convertToString(currentTimeInMillis);
    OffsetDateTime offsetDateTime = GraphQLDateTimeScalar.parseOffsetDateTime(dateAsString, null);
    String expectedFormattedDateTime = GraphQLDateTimeScalar.formatDateTime(offsetDateTime);
    String actualFormattedDateTime = (String) GraphQLDateTimeScalar.type.getCoercing().serialize(dateAsString);
    assertThat(expectedFormattedDateTime).isEqualTo(actualFormattedDateTime);
  }
}
