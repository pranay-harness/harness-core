package io.harness;

import static io.harness.rule.OwnerRule.RUSHABH;

import graphql.schema.CoercingParseLiteralException;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import software.wings.WingsBaseTest;
import software.wings.graphql.scalar.NumberScalar;

public class NumberScalarTest extends WingsBaseTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseLiteralWithInvalidInput() {
    String invalidInput = "invalid";
    thrown.expect(CoercingParseLiteralException.class);
    NumberScalar.type.getCoercing().parseLiteral(invalidInput);
    NumberScalar.type.getCoercing().parseValue(invalidInput);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseLiteralWithValidInput() {
    String validInput = "2";
    Object number = NumberScalar.type.getCoercing().parseLiteral(validInput);
    Assertions.assertThat(number).isInstanceOf(Integer.class);

    Integer validInputInteger = 2;
    number = NumberScalar.type.getCoercing().parseLiteral(validInput);
    Assertions.assertThat(number).isInstanceOf(Integer.class);

    Long validInputLong = 2L;
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLong);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    String validInputLongString = Long.toString(2L);
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLong);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    validInputLongString = Long.toString(System.currentTimeMillis());
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLongString);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    String validInputDoubleString = Double.toString(1.3345);
    number = NumberScalar.type.getCoercing().parseLiteral(validInputDoubleString);
    Assertions.assertThat(number).isInstanceOf(Double.class);

    Double validInputDouble = -1.3345;
    number = NumberScalar.type.getCoercing().parseLiteral(validInputDouble);
    Assertions.assertThat(number).isInstanceOf(Double.class);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseValueWithValidInput() {
    String validInput = "2";
    Object number = NumberScalar.type.getCoercing().parseValue(validInput);
    Assertions.assertThat(number).isInstanceOf(Integer.class);

    Integer validInputInteger = 2;
    number = NumberScalar.type.getCoercing().parseValue(validInput);
    Assertions.assertThat(number).isInstanceOf(Integer.class);

    Long validInputLong = 2L;
    number = NumberScalar.type.getCoercing().parseValue(validInputLong);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    String validInputLongString = Long.toString(2L);
    number = NumberScalar.type.getCoercing().parseValue(validInputLong);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    validInputLongString = Long.toString(System.currentTimeMillis());
    number = NumberScalar.type.getCoercing().parseValue(validInputLongString);
    Assertions.assertThat(number).isInstanceOf(Long.class);

    String validInputDoubleString = Double.toString(1.3345);
    number = NumberScalar.type.getCoercing().parseValue(validInputDoubleString);
    Assertions.assertThat(number).isInstanceOf(Double.class);

    Double validInputDouble = -1.3345;
    number = NumberScalar.type.getCoercing().parseValue(validInputDouble);
    Assertions.assertThat(number).isInstanceOf(Double.class);
  }
}
