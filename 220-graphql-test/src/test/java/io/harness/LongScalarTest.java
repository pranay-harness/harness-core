/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.RUSHABH;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.graphql.scalar.LongScalar;

import graphql.schema.CoercingParseLiteralException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

public class LongScalarTest extends WingsBaseTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseLiteralWithInvalidInput() {
    String invalidInput = "invalid";
    thrown.expect(CoercingParseLiteralException.class);
    LongScalar.type.getCoercing().parseLiteral(invalidInput);
    LongScalar.type.getCoercing().parseValue(invalidInput);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseLiteralWithValidInput() {
    String validInput = "2";
    LongScalar.type.getCoercing().parseLiteral(validInput);

    Long validInputLong = 2L;
    LongScalar.type.getCoercing().parseLiteral(validInputLong);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testParseValueWithValidInput() {
    String validInput = "2";
    LongScalar.type.getCoercing().parseValue(validInput);

    Long validInputLong = 2L;
    LongScalar.type.getCoercing().parseValue(validInputLong);
  }
}
