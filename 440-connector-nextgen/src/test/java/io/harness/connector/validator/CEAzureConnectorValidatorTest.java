package io.harness.connector.validator;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEAzureConnectorValidatorTest extends CategoryTest {
  @InjectMocks CEAzureConnectorValidator ceAzureConnectorValidator;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testValidate() {
    final ConnectorValidationResult connectorValidationResult =
        ceAzureConnectorValidator.validate(null, null, null, null, null);

    assertThat(connectorValidationResult.getStatus()).isEqualTo(ConnectivityStatus.SUCCESS);
    assertThat(connectorValidationResult.getErrors()).isNullOrEmpty();
    assertThat(connectorValidationResult.getErrorSummary()).isNullOrEmpty();
    assertThat(connectorValidationResult.getDelegateId()).isNullOrEmpty();
    assertThat(connectorValidationResult.getTestedAt()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
  }
}