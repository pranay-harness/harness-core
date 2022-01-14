package io.harness.connector.mappers.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.pagerduty.PagerDutyConnector;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class PagerDutyEntityToDTOTest extends CategoryTest {
  @InjectMocks PagerDutyEntityToDTO pagerDutyEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreatePagerDutyConnectorDTO() {
    String encryptedApiToken = "encryptedApiToken";

    PagerDutyConnector pagerDutyConnector = PagerDutyConnector.builder().apiTokenRef(encryptedApiToken).build();

    PagerDutyConnectorDTO pagerDutyConnectorDTO = pagerDutyEntityToDTO.createConnectorDTO(pagerDutyConnector);
    assertThat(pagerDutyConnectorDTO).isNotNull();
    assertThat(pagerDutyConnectorDTO.getApiTokenRef().getIdentifier()).isEqualTo(pagerDutyConnector.getApiTokenRef());
  }
}
