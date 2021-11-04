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
    when(secretStorage.getSecretBy("secret-reference-1")).thenReturn("secret-1");
    when(secretStorage.getSecretBy("secret-reference-2")).thenReturn("secret-2");
    when(secretStorage.getSecretBy("inner-secret-reference-1")).thenReturn("inner-secret-1");

    configSecretResolver = new ConfigSecretResolver(secretStorage);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldNotResolveFieldsWhichAreNotAnnotated() throws IOException {
    // Given
    WorkingConfiguration configuration = new WorkingConfiguration();

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.regularValue()).isEqualTo("regular-value");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldResolveSecretReferenceWithValueFromSecretStorage() throws IOException {
    // Given
    WorkingConfiguration configuration = new WorkingConfiguration();

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.secret1String()).isEqualTo("secret-1");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldResolveSecretReferenceWithValueFromSecretStorageForCharArrayField() throws IOException {
    // Given
    WorkingConfiguration configuration = new WorkingConfiguration();

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(String.valueOf(configuration.secret2CharArray())).isEqualTo("secret-2");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForFinalFields() throws IOException {
    assertThatThrownBy(() -> {
      // Given
      FinalConfiguration configuration = new FinalConfiguration();

      // When
      configSecretResolver.resolveSecret(configuration);
    })
        .isInstanceOf(ConfigSecretException.class)
        .hasMessageContaining(ConfigSecret.class.getSimpleName() + "' can't be used on final fields");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleInnerObjects() throws IOException {
    // Given
    WorkingConfiguration configuration = new WorkingConfiguration();

    // When
    configSecretResolver.resolveSecret(configuration);

    // Then
    assertThat(configuration.getInner().getInnerSecret1()).isEqualTo("inner-secret-1");
    assertThat(configuration.getInner().getInnerRegular()).isEqualTo("inner-regular-value");
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForConfigurationWithoutAnnotations() throws IOException {
    assertThatThrownBy(() -> {
      // Given
      NotWorkingConfiguration configuration = new NotWorkingConfiguration();

      // When
      configSecretResolver.resolveSecret(configuration);
    })
        .isInstanceOf(ConfigSecretException.class)
        .hasMessageContaining("doesn't contain any fields annotated with 'ConfigSecret'");
  }

  public static class WorkingConfiguration {
    @ConfigSecret private String secret1String = "secret-reference-1";
    @ConfigSecret private char[] secret2CharArray = "secret-reference-2".toCharArray();
    private String regularValue = "regular-value";
    @ConfigSecret private InnerWorkingConfiguration inner = new InnerWorkingConfiguration();

    public String secret1String() {
      return secret1String;
    }

    public String regularValue() {
      return regularValue;
    }

    public InnerWorkingConfiguration getInner() {
      return inner;
    }

    public char[] secret2CharArray() {
      return secret2CharArray;
    }
  }

  public static class InnerWorkingConfiguration {
    @ConfigSecret private String innerSecret1 = "inner-secret-reference-1";
    private String innerRegular = "inner-regular-value";

    public String getInnerSecret1() {
      return innerSecret1;
    }

    public String getInnerRegular() {
      return innerRegular;
    }
  }

  private static class FinalConfiguration {
    @ConfigSecret private final String finalFieldToBeResolved = "some-secret-reference";

    public String getFinalFieldToBeResolved() {
      return finalFieldToBeResolved;
    }
  }

  private static class NotWorkingConfiguration {
    @ConfigSecret private EmptyConfig emptyConfig = new EmptyConfig();

    public EmptyConfig getEmptyConfig() {
      return emptyConfig;
    }
  }

  private static class EmptyConfig {
    private String notAnnotated = "dummy";

    public String getNotAnnotated() {
      return notAnnotated;
    }
  }
}
