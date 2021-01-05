package io.harness.connector.validator;

import static io.harness.rule.OwnerRule.UTSAV;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEAwsConnectorValidatorTest extends CategoryTest {
  @InjectMocks private CEAwsConnectorValidator connectorValidator;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testValidate() {
    // TODO(UTSAV): implement it after CEAwsConnectorValidator
  }
}