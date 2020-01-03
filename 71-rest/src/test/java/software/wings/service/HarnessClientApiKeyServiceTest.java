package software.wings.service;

import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.service.intfc.HarnessApiKeyService;

/**
 *
 * @author rktummala
 */
public class HarnessClientApiKeyServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private HarnessApiKeyService harnessApiKeyService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testCRUD() {
    String generatedApiKey = harnessApiKeyService.generate(ClientType.PROMETHEUS.name());
    assertThat(generatedApiKey).isNotNull();

    String keyFromGet = harnessApiKeyService.get(ClientType.PROMETHEUS.name());
    assertThat(generatedApiKey).isEqualTo(keyFromGet);

    String salesForceKey = harnessApiKeyService.generate(ClientType.SALESFORCE.name());
    assertThat(generatedApiKey).isNotEqualTo(salesForceKey);

    thrown.expect(Exception.class);
    harnessApiKeyService.generate(ClientType.PROMETHEUS.name());

    harnessApiKeyService.delete(ClientType.PROMETHEUS.name());

    thrown.expect(Exception.class);
    harnessApiKeyService.get(ClientType.PROMETHEUS.name());
  }
}
