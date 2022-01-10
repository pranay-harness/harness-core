/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.trigger;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.beans.trigger.WebhookEventType.OTHER;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookEventType.PUSH;
import static software.wings.beans.trigger.WebhookParameters.GH_PR_ID;
import static software.wings.beans.trigger.WebhookParameters.GH_PUSH_REF;
import static software.wings.beans.trigger.WebhookParameters.GIT_LAB_PUSH_REF;
import static software.wings.beans.trigger.WebhookParameters.PULL_REQUEST_ID;
import static software.wings.beans.trigger.WebhookParameters.suggestExpressions;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;
import static software.wings.beans.trigger.WebhookSource.GITLAB;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookParametersTest extends CategoryTest {
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldSuggestExpressions() {
    assertThat(suggestExpressions(BITBUCKET, PULL_REQUEST).contains(PULL_REQUEST_ID)).isTrue();
    assertThat(suggestExpressions(BITBUCKET, PUSH)).isEmpty();

    assertThat(suggestExpressions(GITHUB, PULL_REQUEST).contains(GH_PR_ID)).isTrue();
    assertThat(suggestExpressions(GITHUB, PUSH).contains(GH_PUSH_REF)).isTrue();
    assertThat(suggestExpressions(GITHUB, OTHER)).isEmpty();

    assertThat(suggestExpressions(GITLAB, PULL_REQUEST)).isEmpty();
    assertThat(suggestExpressions(GITLAB, PUSH).contains(GIT_LAB_PUSH_REF)).isTrue();
  }
}
