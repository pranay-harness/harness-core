package io.harness.secret;

import static io.harness.rule.OwnerRule.FILIP;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigSecretResolverUnitTest extends CategoryTest {
  @Mock private SecretStorage secretStorage;
  private ConfigSecretResolver configSecretResolver;

  @Before
  public void setUp() {
    configSecretResolver = new ConfigSecretResolver(secretStorage);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldReplaceSecretReferenceWithValueFromSecretStorage() throws IOException {
    // Given
    DummyConfiguration configuration = new DummyConfiguration();

    when(secretStorage.getSecretBy("dummy-secret-reference")).thenReturn("secret-from-secret-manager");

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.getSecret()).isEqualTo("secret-from-secret-manager");
  }

  public static class DummyConfiguration {
    @ConfigSecret private final String secret;

    public DummyConfiguration() {
      secret = "dummy-secret-reference";
    }

    public String getSecret() {
      return secret;
    }
  }
}
