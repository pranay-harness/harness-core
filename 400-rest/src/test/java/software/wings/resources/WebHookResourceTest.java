package software.wings.resources;

import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.security.annotations.ApiKeyAuthorized;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebHookResourceTest extends WingsBaseTest {
  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testWebhookResourceHasApiKeyAuthorizedAnnotation() {
    Class<?> resourceClass = WebHookResource.class;

    ApiKeyAuthorized[] classAnnotations = resourceClass.getAnnotationsByType(ApiKeyAuthorized.class);
    assertThat(classAnnotations).isNotEmpty();
    ApiKeyAuthorized classAnnotation = classAnnotations[0];
    assertThat(classAnnotation).isNotNull();
    assertThat(classAnnotation.allowEmptyApiKey()).isTrue();
    assertThat(classAnnotation.skipAuth()).isTrue();
  }
}
