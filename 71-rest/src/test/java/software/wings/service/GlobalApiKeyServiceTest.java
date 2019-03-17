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
import software.wings.beans.GlobalApiKey.ProviderType;
import software.wings.service.intfc.GlobalApiKeyService;

/**
 *
 * @author rktummala
 */
public class GlobalApiKeyServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private GlobalApiKeyService globalApiKeyService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Category(UnitTests.class)
  public void testCRUD() {
    String generatedApiKey = globalApiKeyService.generate(ProviderType.PROMETHEUS);
    assertNotNull(generatedApiKey);

    String keyFromGet = globalApiKeyService.get(ProviderType.PROMETHEUS);
    assertEquals(keyFromGet, generatedApiKey);

    String salesForceKey = globalApiKeyService.generate(ProviderType.SALESFORCE);
    assertNotEquals(generatedApiKey, salesForceKey);

    thrown.expect(Exception.class);
    globalApiKeyService.generate(ProviderType.PROMETHEUS);

    globalApiKeyService.delete(ProviderType.PROMETHEUS);

    thrown.expect(Exception.class);
    globalApiKeyService.get(ProviderType.PROMETHEUS);
  }
}
