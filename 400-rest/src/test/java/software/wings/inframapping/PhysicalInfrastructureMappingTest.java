/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.inframapping;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping.Builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PhysicalInfrastructureMappingTest extends WingsBaseTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplyProvisionerVariables() {
    PhysicalInfrastructureMapping infrastructureMapping = Builder.aPhysicalInfrastructureMapping().build();

    HashMap<String, Object> blueprintProperties = new HashMap<>();

    Map<String, Object> host1 = Maps.newLinkedHashMap();
    host1.put("Hostname", "abc.com");
    host1.put("amiId", 123);

    Map<String, Object> host2 = Maps.newLinkedHashMap();
    host2.put("Hostname", "abcd.com");
    host2.put("amiId", 1234);

    List<Map<String, Object>> hosts = Lists.newArrayList(host1, host2);
    blueprintProperties.put("hostArrayPath", hosts);

    infrastructureMapping.applyProvisionerVariables(blueprintProperties, null, false);

    assertThat(infrastructureMapping.hosts()).hasSize(2);
    assertThat(infrastructureMapping.hosts().get(0).getPublicDns()).isEqualTo("abc.com");
    assertThat(infrastructureMapping.hosts().get(0).getProperties().get("amiId")).isEqualTo(123);
    assertThat(infrastructureMapping.hosts().get(1).getPublicDns()).isEqualTo("abcd.com");
    assertThat(infrastructureMapping.hosts().get(1).getProperties().get("amiId")).isEqualTo(1234);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(null, null, false));

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(Collections.EMPTY_MAP, null, false));

    Map<String, Object> host3 = Maps.newLinkedHashMap();
    host2.put("amiId", 1234);
    hosts.add(host3);

    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> infrastructureMapping.applyProvisionerVariables(new HashMap<>(), null, false));
  }
}
