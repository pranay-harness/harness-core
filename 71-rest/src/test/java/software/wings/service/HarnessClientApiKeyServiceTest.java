package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
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
  @Category(UnitTests.class)
  public void testCRUD() {
    String generatedApiKey = harnessApiKeyService.generate(ClientType.PROMETHEUS.name());
    assertNotNull(generatedApiKey);

    String keyFromGet = harnessApiKeyService.get(ClientType.PROMETHEUS.name());
    assertEquals(keyFromGet, generatedApiKey);

    String salesForceKey = harnessApiKeyService.generate(ClientType.SALESFORCE.name());
    assertNotEquals(generatedApiKey, salesForceKey);

    thrown.expect(Exception.class);
    harnessApiKeyService.generate(ClientType.PROMETHEUS.name());

    harnessApiKeyService.delete(ClientType.PROMETHEUS.name());

    thrown.expect(Exception.class);
    harnessApiKeyService.get(ClientType.PROMETHEUS.name());
  }
}
