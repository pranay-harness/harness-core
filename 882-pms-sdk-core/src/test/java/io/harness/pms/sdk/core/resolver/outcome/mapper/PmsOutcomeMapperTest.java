/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.resolver.outcome.mapper;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PIPELINE)
@RunWith(PowerMockRunner.class)
@PrepareForTest({RecastOrchestrationUtils.class})
public class PmsOutcomeMapperTest extends PmsSdkCoreTestBase {
  @Before
  public void initialize() {
    PowerMockito.mockStatic(RecastOrchestrationUtils.class);
    PowerMockito.when(RecastOrchestrationUtils.toJson(any(Outcome.class))).thenReturn("test");
    PowerMockito.when(RecastOrchestrationUtils.fromJson(any(String.class), eq(Outcome.class)))
        .thenReturn(DummyOutcome.builder().name("dummyOutcome").build());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertOutcomeValueToJson() {
    DummyOutcome dummyOutcome = DummyOutcome.builder().name("dummyOutcome").build();
    assertThat(PmsOutcomeMapper.convertOutcomeValueToJson(dummyOutcome)).isEqualTo("test");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertJsonToOutcome() {
    DummyOutcome dummyOutcome = DummyOutcome.builder().name("dummyOutcome").build();
    String json = null;
    assertThat(PmsOutcomeMapper.convertJsonToOutcome(json)).isNull();
    json = "test";
    assertThat(PmsOutcomeMapper.convertJsonToOutcome(json)).isEqualTo(dummyOutcome);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertJsonToOutcomeList() {
    List<String> jsons = Arrays.asList(null, "test");
    assertThat(PmsOutcomeMapper.convertJsonToOutcome(jsons)).isNotNull();
    assertThat(PmsOutcomeMapper.convertJsonToOutcome(jsons).size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertJsonToOrchestrationMap() {
    Map<String, String> jsons = new HashMap<>();
    jsons.put("key", "test");
    assertThat(PmsOutcomeMapper.convertJsonToOrchestrationMap(jsons)).isNotNull();
    assertThat(PmsOutcomeMapper.convertJsonToOrchestrationMap(jsons).size()).isEqualTo(1);
  }

  @Data
  @Builder
  public static class DummyOutcome implements Outcome {
    String name;
  }
}
