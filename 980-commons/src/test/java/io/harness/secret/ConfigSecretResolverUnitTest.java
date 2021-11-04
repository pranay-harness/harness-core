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
  public void setUp() throws IOException {
    configSecretResolver = new ConfigSecretResolver(secretStorage);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldNotResolveFieldsWhichAreNotAnnotated() throws IOException {
    // Given
    DummyConfiguration configuration = new DummyConfiguration();

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.getShouldNotBeResolved()).isEqualTo("should-not-resolve");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldResolveSecretReferenceWithValueFromSecretStorage() throws IOException {
    // Given
    DummyConfiguration configuration = new DummyConfiguration();

    when(secretStorage.getSecretBy("some-secret-reference")).thenReturn("secret-from-secret-manager");

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.getToBeResolved()).isEqualTo("secret-from-secret-manager");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleInnerObjects() throws IOException {
    // Given
    DummyConfiguration configuration = new DummyConfiguration();

    when(secretStorage.getSecretBy("some-inner-secret-reference")).thenReturn("inner-secret");

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.getInner().getInnerSecret()).isEqualTo("inner-secret");
    assertThat(configuration.getInner().getInnerRegular()).isEqualTo("regular-value");
  }

  public static class DummyConfiguration {
    @ConfigSecret private String toBeResolved = "some-secret-reference";
    private String shouldNotBeResolved = "should-not-resolve";
    @ConfigSecret private InnerDummyConfiguration inner = new InnerDummyConfiguration();

    public String getToBeResolved() {
      return toBeResolved;
    }

    public String getShouldNotBeResolved() {
      return shouldNotBeResolved;
    }

    public InnerDummyConfiguration getInner() {
      return inner;
    }
  }

  public static class InnerDummyConfiguration {
    @ConfigSecret private String innerSecret = "some-inner-secret-reference";
    private String innerRegular = "regular-value";

    public String getInnerSecret() {
      return innerSecret;
    }

    public String getInnerRegular() {
      return innerRegular;
    }
  }
}
