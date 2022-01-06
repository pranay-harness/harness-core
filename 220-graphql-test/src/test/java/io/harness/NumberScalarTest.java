/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.graphql.scalar.NumberScalar;

import graphql.schema.CoercingParseLiteralException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

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
    assertThat(number).isInstanceOf(Integer.class);

    Integer validInputInteger = 2;
    number = NumberScalar.type.getCoercing().parseLiteral(validInput);
    assertThat(number).isInstanceOf(Integer.class);

    Long validInputLong = 2L;
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLong);
    assertThat(number).isInstanceOf(Long.class);

    String validInputLongString = Long.toString(2L);
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLong);
    assertThat(number).isInstanceOf(Long.class);

    validInputLongString = Long.toString(System.currentTimeMillis());
    number = NumberScalar.type.getCoercing().parseLiteral(validInputLongString);
    assertThat(number).isInstanceOf(Long.class);

    String validInputDoubleString = Double.toString(1.3345);
    number = NumberScalar.type.getCoercing().parseLiteral(validInputDoubleString);
    assertThat(number).isInstanceOf(Double.class);

    Double validInputDouble = -1.3345;
    number = NumberScalar.type.getCoercing().parseLiteral(validInputDouble);
    assertThat(number).isInstanceOf(Double.class);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseValueWithValidInput() {
    String validInput = "2";
    Object number = NumberScalar.type.getCoercing().parseValue(validInput);
    assertThat(number).isInstanceOf(Integer.class);

    Integer validInputInteger = 2;
    number = NumberScalar.type.getCoercing().parseValue(validInput);
    assertThat(number).isInstanceOf(Integer.class);

    Long validInputLong = 2L;
    number = NumberScalar.type.getCoercing().parseValue(validInputLong);
    assertThat(number).isInstanceOf(Long.class);

    String validInputLongString = Long.toString(2L);
    number = NumberScalar.type.getCoercing().parseValue(validInputLong);
    assertThat(number).isInstanceOf(Long.class);

    validInputLongString = Long.toString(System.currentTimeMillis());
    number = NumberScalar.type.getCoercing().parseValue(validInputLongString);
    assertThat(number).isInstanceOf(Long.class);

    String validInputDoubleString = Double.toString(1.3345);
    number = NumberScalar.type.getCoercing().parseValue(validInputDoubleString);
    assertThat(number).isInstanceOf(Double.class);

    Double validInputDouble = -1.3345;
    number = NumberScalar.type.getCoercing().parseValue(validInputDouble);
    assertThat(number).isInstanceOf(Double.class);
  }
}
