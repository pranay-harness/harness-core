/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessModule._815_CG_TRIGGERS;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.INDER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.security.annotations.ApiKeyAuthorized;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@TargetModule(_815_CG_TRIGGERS)
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
