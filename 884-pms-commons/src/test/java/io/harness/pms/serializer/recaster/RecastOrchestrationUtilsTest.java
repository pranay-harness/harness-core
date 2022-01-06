/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.serializer.recaster;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.ParameterFieldValueWrapper;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class RecastOrchestrationUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonForNull() {
    String simpleJson = RecastOrchestrationUtils.toSimpleJson(null);
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo("{}");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJson() {
    DummyB dummyB = DummyB.builder()
                        .pf(ParameterField.createValueField("value"))
                        .intVal(1)
                        .longVal(1242352345234L)
                        .doubleVal(12423532.235254)
                        .listVal(Lists.newArrayList("a", "b", "c"))
                        .mapVal(Maps.newHashMap(ImmutableMap.of("1", 1)))
                        .strVal("string")
                        .build();
    String simpleJson = RecastOrchestrationUtils.toSimpleJson(dummyB);

    String expectedValue =
        "{\"strVal\":\"string\",\"intVal\":1,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":1},\"pf\":\"value\"}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithListAsAField() {
    DummyB dummyB = DummyB.builder()
                        .pf(ParameterField.createValueField("value"))
                        .intVal(1)
                        .longVal(1242352345234L)
                        .doubleVal(12423532.235254)
                        .listVal(Lists.newArrayList("a", "b", "c"))
                        .mapVal(Maps.newHashMap(ImmutableMap.of("1", 1)))
                        .strVal("string")
                        .build();

    DummyB dummyB1 = DummyB.builder()
                         .pf(ParameterField.createValueField("value"))
                         .intVal(2)
                         .longVal(1242352345234L)
                         .doubleVal(12423532.235254)
                         .listVal(Lists.newArrayList("a", "b", "c"))
                         .mapVal(Maps.newHashMap(ImmutableMap.of("1", 2)))
                         .strVal("string")
                         .build();

    DummyA dummyA = new DummyA("dasgsregww", 12355235235L, Lists.newArrayList(dummyB, dummyB1));

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(dummyA);

    String expectedValue =
        "{\"strVal\":\"dasgsregww\",\"aLong\":12355235235,\"list\":[{\"strVal\":\"string\",\"intVal\":1,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":1},\"pf\":\"value\"},{\"strVal\":\"string\",\"intVal\":2,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":2},\"pf\":\"value\"}]}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithParameterField() {
    DummyB dummyB = DummyB.builder()
                        .pf(ParameterField.createValueField("value"))
                        .intVal(1)
                        .longVal(1242352345234L)
                        .doubleVal(12423532.235254)
                        .listVal(Lists.newArrayList("a", "b", "c"))
                        .mapVal(Maps.newHashMap(ImmutableMap.of("1", 1)))
                        .strVal("string")
                        .build();

    DummyB dummyB1 = DummyB.builder()
                         .pf(ParameterField.createValueField("value"))
                         .intVal(2)
                         .longVal(1242352345234L)
                         .doubleVal(12423532.235254)
                         .listVal(Lists.newArrayList("a", "b", "c"))
                         .mapVal(Maps.newHashMap(ImmutableMap.of("1", 2)))
                         .strVal("string")
                         .build();

    ParameterDummy parameterDummy =
        ParameterDummy.builder().list(ParameterField.createValueField(Lists.newArrayList(dummyB, dummyB1))).build();

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(parameterDummy);

    String expectedValue =
        "{\"list\":[{\"strVal\":\"string\",\"intVal\":1,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":1},\"pf\":\"value\"},{\"strVal\":\"string\",\"intVal\":2,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":2},\"pf\":\"value\"}]}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithEncodedValue() {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();
    ProtoAsAFieldClass protoAsAFieldClass =
        ProtoAsAFieldClass.builder()
            .executionErrorInfo(executionErrorInfo)
            .failureTypeSet(Sets.newHashSet(FailureType.APPLICATION_FAILURE, FailureType.AUTHORIZATION_FAILURE))
            .build();

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(protoAsAFieldClass);

    String expectedValue =
        "{\"executionErrorInfo\":{\"message\":\"some-message\"},\"failureTypeSet\":[\"APPLICATION_FAILURE\",\"AUTHORIZATION_FAILURE\"]}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithEncodedClass() {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(executionErrorInfo);

    String expectedValue = "{\n"
        + "  \"message\": \"some-message\"\n"
        + "}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithList() {
    DummyB dummyB = DummyB.builder()
                        .pf(ParameterField.createValueField("value"))
                        .intVal(1)
                        .longVal(1242352345234L)
                        .doubleVal(12423532.235254)
                        .listVal(Lists.newArrayList("a", "b", "c"))
                        .mapVal(Maps.newHashMap(ImmutableMap.of("1", 1)))
                        .strVal("string")
                        .build();

    DummyB dummyB1 = DummyB.builder()
                         .pf(ParameterField.createValueField("value"))
                         .intVal(2)
                         .longVal(1242352345234L)
                         .doubleVal(12423532.235254)
                         .listVal(Lists.newArrayList("a", "b", "c"))
                         .mapVal(Maps.newHashMap(ImmutableMap.of("1", 2)))
                         .strVal("string")
                         .build();

    List<DummyB> list = Lists.newArrayList(dummyB, dummyB1);

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(list);

    String expectedValue =
        "[{\"strVal\":\"string\",\"intVal\":1,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":1},\"pf\":\"value\"},{\"strVal\":\"string\",\"intVal\":2,\"longVal\":1242352345234,\"doubleVal\":1.2423532235254E7,\"listVal\":[\"a\",\"b\",\"c\"],\"mapVal\":{\"1\":2},\"pf\":\"value\"}]";

    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithListOfEncodedObjects() {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();
    ProtoAsAFieldClass protoAsAFieldClass =
        ProtoAsAFieldClass.builder()
            .executionErrorInfo(executionErrorInfo)
            .failureTypeSet(Sets.newHashSet(FailureType.APPLICATION_FAILURE, FailureType.AUTHORIZATION_FAILURE))
            .build();

    ExecutionErrorInfo executionErrorInfo1 = ExecutionErrorInfo.newBuilder().setMessage("some-message1").build();
    ProtoAsAFieldClass protoAsAFieldClass1 =
        ProtoAsAFieldClass.builder()
            .executionErrorInfo(executionErrorInfo1)
            .failureTypeSet(Sets.newHashSet(FailureType.AUTHENTICATION_FAILURE, FailureType.CONNECTIVITY_FAILURE))
            .build();

    List<ProtoAsAFieldClass> list = Lists.newArrayList(protoAsAFieldClass, protoAsAFieldClass1);

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(list);

    String expectedValue =
        "[{\"executionErrorInfo\":{\"message\":\"some-message\"},\"failureTypeSet\":[\"APPLICATION_FAILURE\",\"AUTHORIZATION_FAILURE\"]},{\"executionErrorInfo\":{\"message\":\"some-message1\"},\"failureTypeSet\":[\"AUTHENTICATION_FAILURE\",\"CONNECTIVITY_FAILURE\"]}]";

    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestToSimpleJsonWithMap() {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();
    ExecutionErrorInfo executionErrorInfo1 = ExecutionErrorInfo.newBuilder().setMessage("some-message1").build();
    ExecutionErrorInfo executionErrorInfo2 = ExecutionErrorInfo.newBuilder().setMessage("some-message2").build();

    Map<String, ExecutionErrorInfo> executionErrorInfoMap =
        ImmutableMap.of("0", executionErrorInfo, "1", executionErrorInfo1, "2", executionErrorInfo2);

    String simpleJson = RecastOrchestrationUtils.toSimpleJson(executionErrorInfoMap);

    String expectedValue =
        "{\"0\":{\"message\":\"some-message\"},\"1\":{\"message\":\"some-message1\"},\"2\":{\"message\":\"some-message2\"}}";
    assertThat(simpleJson).isNotNull();
    assertThat(simpleJson).isEqualTo(expectedValue);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestGenericMapInsideWrapper() {
    DummyC dummyC = new DummyC(
        new ParameterFieldValueWrapper<>(ImmutableMap.of("a", 123, "b", ParameterField.createValueField(123))));
    Map<String, Object> map = RecastOrchestrationUtils.toMap(dummyC);
    assertThat(map).isNotNull();

    DummyC dummyC1 = RecastOrchestrationUtils.fromMap(map, DummyC.class);
    assertThat(dummyC1).isNotNull();
    assertThat(dummyC1).isEqualTo(dummyC);
  }

  @Data
  @Builder
  public static class DummyB {
    private String strVal;
    private int intVal;
    private long longVal;
    private double doubleVal;
    private List<String> listVal;
    private Map<String, Integer> mapVal;
    private ParameterField<String> pf;
  }

  @Data
  @AllArgsConstructor
  public static class DummyA {
    private String strVal;
    private long aLong;
    private List<DummyB> list;
  }

  @Data
  @AllArgsConstructor
  public static class DummyC {
    ParameterFieldValueWrapper<Map<String, Object>> wrapper;
  }

  @Data
  @Builder
  public static class ParameterDummy {
    private ParameterField<List<DummyB>> list;
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ProtoAsAFieldClass {
    private ExecutionErrorInfo executionErrorInfo;
    private Set<FailureType> failureTypeSet;
  }
}
