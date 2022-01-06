/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.rule.OwnerRule.ABOSII;

import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.NAMESPACE;
import static software.wings.utils.WingsTestConstants.RELEASE_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.infra.DirectKubernetesInfrastructure.DirectKubernetesInfrastructureKeys;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DirectKubernetesInfrastructureTest extends CategoryTest {
  private final DirectKubernetesInfrastructure infrastructure =
      DirectKubernetesInfrastructure.builder().cloudProviderId(COMPUTE_PROVIDER_ID).build();

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetSupportedExpressions() {
    assertThat(infrastructure.getSupportedExpressions())
        .containsExactlyInAnyOrder(
            DirectKubernetesInfrastructureKeys.namespace, DirectKubernetesInfrastructureKeys.releaseName);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyExpressions() {
    Map<String, Object> expressions = new HashMap<>();
    expressions.put(DirectKubernetesInfrastructureKeys.namespace, 12345);
    expressions.put(DirectKubernetesInfrastructureKeys.releaseName, 12345);

    assertThatThrownBy(() -> infrastructure.applyExpressions(expressions, APP_ID, ENV_ID, INFRA_DEFINITION_ID))
        .isInstanceOf(InvalidRequestException.class);
    // release name still has an invalid value
    expressions.put(DirectKubernetesInfrastructureKeys.namespace, NAMESPACE);
    assertThatThrownBy(() -> infrastructure.applyExpressions(expressions, APP_ID, ENV_ID, INFRA_DEFINITION_ID))
        .isInstanceOf(InvalidRequestException.class);

    expressions.put(DirectKubernetesInfrastructureKeys.releaseName, RELEASE_NAME);
    infrastructure.applyExpressions(expressions, APP_ID, ENV_ID, INFRA_DEFINITION_ID);
    assertThat(infrastructure.getNamespace()).isEqualTo(NAMESPACE);
    assertThat(infrastructure.getReleaseName()).isEqualTo(RELEASE_NAME);
  }
}
