/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.infrastructuredefinition;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.infra.AwsAmiInfrastructure;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class InfrastructureDefinitionHelperTest extends WingsBaseTest {
  @InjectMocks private InfrastructureDefinitionHelper infrastructureDefinitionHelper;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetQueryMapWhenNoUserDefinedFields() {
    AwsAmiInfrastructure awsAmiInfrastructure = AwsAmiInfrastructure.builder().build();

    Map<String, Object> queryMap = infrastructureDefinitionHelper.getQueryMap(awsAmiInfrastructure);

    assertThat(queryMap.keySet()).hasSize(2);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetQueryMapWithNoUserDefinedFields() {
    AwsAmiInfrastructure awsAmiInfrastructure = AwsAmiInfrastructure.builder().asgIdentifiesWorkload(true).build();

    Map<String, Object> queryMap = infrastructureDefinitionHelper.getQueryMap(awsAmiInfrastructure);

    assertThat(queryMap.keySet()).hasSize(3);
  }
}
