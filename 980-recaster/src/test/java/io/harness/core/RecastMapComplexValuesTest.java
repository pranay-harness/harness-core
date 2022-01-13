/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.RecasterTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exceptions.RecasterException;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecastMapComplexValuesTest extends RecasterTestBase {
  private static final String RECAST_KEY = Recaster.RECAST_CLASS_KEY;
  private Recaster recaster;

  @Before
  public void setup() {
    recaster = new Recaster(RecasterOptions.builder().workWithMaps(true).build());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithEnum() {
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyEnum.class));
    DummyEnum dummyEnum = DummyEnum.builder().types(Collections.singletonList(DummyEnum.Type.SUPER_DUMMY)).build();

    Map<String, Object> document = recast.toMap(dummyEnum);
    assertThat(document).isNotEmpty();
    assertThat(document.get("types")).isEqualTo(Collections.singletonList(DummyEnum.Type.SUPER_DUMMY.name()));

    DummyEnum recastedDummyEnum = recast.fromMap(document, DummyEnum.class);
    assertThat(recastedDummyEnum).isNotNull();
    assertThat(recastedDummyEnum.types).isEqualTo(Collections.singletonList(DummyEnum.Type.SUPER_DUMMY));
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestRecasterThrowException() {
    Recast recast = new Recast(recaster, ImmutableSet.of(DummySimpleSet.class));

    Map<String, Object> document = recast.toMap(DummySimpleList.builder().build());
    document.put("__recast", "randomClassPath");
    assertThatThrownBy(() -> recast.fromMap(document, DummySimpleList.class)).isInstanceOf(RecasterException.class);
  }

  @Builder
  @AllArgsConstructor
  private static class DummyEnum {
    private List<Type> types;
    private enum Type { SUPER_DUMMY }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithSimpleMap() {
    final Map<String, String> map = new HashMap<>();
    map.put("keyparam", "value");
    map.put("key.param", "value");

    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> recastedMap = recast.toMap(map);
    assertThat(recastedMap).isNotEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithSimpleList() {
    final List<Integer> list = Arrays.asList(1, 2, 3);

    Recast recast = new Recast(recaster, ImmutableSet.of(DummySimpleList.class));
    DummySimpleList dummyList = DummySimpleList.builder().list(list).build();
    Map<String, Object> document = recast.toMap(dummyList);

    assertThat(document).isNotEmpty();
    assertThat(document).isNotEmpty();
    assertThat(document.get("list")).isEqualTo(list);

    DummySimpleList recastedSimpleList = recast.fromMap(document, DummySimpleList.class);
    assertThat(recastedSimpleList).isNotNull();
    assertThat(recastedSimpleList.list).isEqualTo(list);
  }

  @Builder
  @AllArgsConstructor
  private static class DummySimpleList {
    private List<Integer> list;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithSimpleSet() {
    final Set<Integer> set = ImmutableSet.of(1, 2, 3);

    Recast recast = new Recast(recaster, ImmutableSet.of(DummySimpleSet.class));
    DummySimpleSet dummySet = DummySimpleSet.builder().set(set).build();

    Map<String, Object> expected = ImmutableMap.of(RECAST_KEY, DummySimpleSet.class.getName(), "set", set);

    Map<String, Object> document = recast.toMap(dummySet);

    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummySimpleSet recastedSimpleSet = recast.fromMap(document, DummySimpleSet.class);
    assertThat(recastedSimpleSet).isNotNull();
    assertThat(recastedSimpleSet.set).isEqualTo(set);
  }

  @Builder
  @AllArgsConstructor
  private static class DummySimpleSet {
    private Set<Integer> set;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithInterface() {
    ITest iTest = new ITestImpl("someType");
    Recast recast = new Recast(recaster, ImmutableSet.of(ITest.class));

    Map<String, Object> document = recast.toMap(iTest);
    assertThat(document.get(RECAST_KEY)).isEqualTo(ITestImpl.class.getName());
    assertThat(document.get("type")).isEqualTo("someType");

    ITest recastedITest = recast.fromMap(document, ITest.class);
    assertThat(recastedITest).isNotNull();
    assertThat(recastedITest instanceof ITestImpl).isTrue();
    assertThat(recastedITest.getType()).isEqualTo("someType");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfInterfaces() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<ITest> iTests = Arrays.asList(iTest, iTest1, iTest2);
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfInterfaces.class));

    DummyListOfInterfaces dummyListOfInterfaces = DummyListOfInterfaces.builder().iTests(iTests).build();

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY, DummyListOfInterfaces.class.getName(), "iTests",
        ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2")));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromMap(document, DummyListOfInterfaces.class);
    assertThat(recastedDummyListOfInterfaces).isNotNull();
    assertThat(recastedDummyListOfInterfaces.iTests).isEqualTo(iTests);
    for (ITest test : recastedDummyListOfInterfaces.iTests) {
      assertThat(test instanceof ITestImpl).isTrue();
    }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfInterfacesWithoutRegisteringTheClass() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<ITest> iTests = Arrays.asList(iTest, iTest1, iTest2);

    Map<String, Object> document = ImmutableMap.of(RECAST_KEY, DummyListOfInterfaces.class.getName(), "iTests",
        ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2")));

    Recast recast = new Recast(recaster, ImmutableSet.of());

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromMap(document, DummyListOfInterfaces.class);
    assertThat(recastedDummyListOfInterfaces).isNotNull();
    assertThat(recastedDummyListOfInterfaces.iTests).isEqualTo(iTests);
    for (ITest test : recastedDummyListOfInterfaces.iTests) {
      assertThat(test instanceof ITestImpl).isTrue();
    }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfInterfacesWithoutRegisteringTheClassException() {
    Map<String, Object> document = ImmutableMap.of(RECAST_KEY, DummyListOfSetOfInterfaces.class.getName(), "list",
        ImmutableList.of(ImmutableSet.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2"))));

    Recast recast = new Recast(recaster, ImmutableSet.of());

    assertThatThrownBy(() -> recast.fromMap(document, DummyListOfInterfaces.class))
        .isInstanceOf(RecasterException.class)
        .hasMessageContaining("%s class cannot be mapped to %s", DummyListOfSetOfInterfaces.class.getName(),
            DummyListOfInterfaces.class.getName());
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfSetOfInterfaces() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<Set<ITest>> iTests = ImmutableList.of(ImmutableSet.of(iTest, iTest1, iTest2));
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfInterfaces.class));

    DummyListOfSetOfInterfaces dummyListOfInterfaces = DummyListOfSetOfInterfaces.builder().list(iTests).build();

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY, DummyListOfSetOfInterfaces.class.getName(), "list",
        ImmutableList.of(ImmutableSet.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
            ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2"))));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfSetOfInterfaces recasted = recast.fromMap(document, DummyListOfSetOfInterfaces.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted.list).isEqualTo(iTests);
    for (Set<ITest> iTestSet : recasted.list) {
      for (ITest test : iTestSet) {
        assertThat(test instanceof ITestImpl).isTrue();
      }
    }
  }

  @Builder
  @AllArgsConstructor
  private static class DummyListOfSetOfInterfaces {
    private List<Set<ITest>> list;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfSetOfListOfSetInterfaces() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<Set<List<Set<ITest>>>> iTests =
        ImmutableList.of(ImmutableSet.of(ImmutableList.of(ImmutableSet.of(iTest, iTest1, iTest2))));
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfSetOfListOfSetInterfaces.class));

    DummyListOfSetOfListOfSetInterfaces dummyListOfInterfaces =
        DummyListOfSetOfListOfSetInterfaces.builder().list(iTests).build();

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY, DummyListOfSetOfListOfSetInterfaces.class.getName(),
        "list",
        ImmutableList.of(ImmutableSet.of(
            ImmutableList.of(ImmutableSet.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
                ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
                ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2"))))));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfSetOfListOfSetInterfaces recasted = recast.fromMap(document, DummyListOfSetOfListOfSetInterfaces.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted.list).isEqualTo(iTests);
    for (Set<List<Set<ITest>>> setOfListOfSet : recasted.list) {
      for (List<Set<ITest>> listOfSet : setOfListOfSet) {
        for (Set<ITest> set : listOfSet) {
          for (ITest test : set) {
            assertThat(test instanceof ITestImpl).isTrue();
          }
        }
      }
    }
  }

  @Builder
  @AllArgsConstructor
  private static class DummyListOfSetOfListOfSetInterfaces {
    private List<Set<List<Set<ITest>>>> list;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithStringKeyListOfSetOfListOfSetInterfacesValueMap() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<Set<List<Set<ITest>>>> iTests =
        ImmutableList.of(ImmutableSet.of(ImmutableList.of(ImmutableSet.of(iTest, iTest1, iTest2))));

    Map<String, List<Set<List<Set<ITest>>>>> damnMap = ImmutableMap.of("WOOW", iTests);

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListStringKeyOfSetOfListOfSetInterfacesMap.class));

    DummyListStringKeyOfSetOfListOfSetInterfacesMap dummyListOfInterfaces =
        DummyListStringKeyOfSetOfListOfSetInterfacesMap.builder().damnMap(damnMap).build();

    Map<String, Object> expectedDoc =
        ImmutableMap.of(RECAST_KEY, DummyListStringKeyOfSetOfListOfSetInterfacesMap.class.getName(), "damnMap",
            ImmutableMap.of("WOOW",
                ImmutableList.of(ImmutableSet.of(ImmutableList.of(
                    ImmutableSet.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
                        ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1"),
                        ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2")))))));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListStringKeyOfSetOfListOfSetInterfacesMap recasted =
        recast.fromMap(document, DummyListStringKeyOfSetOfListOfSetInterfacesMap.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted.damnMap).isEqualTo(damnMap);
    assertThat(recasted.damnMap.get("WOOW"))
        .contains(ImmutableSet.of(ImmutableList.of(ImmutableSet.of(iTest, iTest1, iTest2))));
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyListStringKeyOfSetOfListOfSetInterfacesMap {
    private Map<String, List<Set<List<Set<ITest>>>>> damnMap;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfInterfaceWithDifferentImplementations() {
    List<Integer> fourArray = Arrays.asList(1, 2, 3, 4);
    List<Integer> fiveArray = Arrays.asList(1, 2, 3, 4, 5);
    Map<String, List<Integer>> iTest1map = new HashMap<>();
    iTest1map.put("four", fourArray);
    iTest1map.put("five", fiveArray);

    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl1("someType1", iTest1map);
    ITest iTest2 = new ITestImpl2("someType2");
    List<ITest> iTests = Arrays.asList(iTest, iTest1, iTest2);
    DummyListOfInterfaces dummyListOfInterfaces = DummyListOfInterfaces.builder().iTests(iTests).build();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfInterfaces.class));

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY, DummyListOfInterfaces.class.getName(), "iTests",
        ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType"),
            ImmutableMap.of(RECAST_KEY, ITestImpl1.class.getName(), "type1", "someType1", "map",
                ImmutableMap.of("four", fourArray, "five", fiveArray)),
            ImmutableMap.of(RECAST_KEY, ITestImpl2.class.getName(), "type2", "someType2")));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromMap(document, DummyListOfInterfaces.class);
    assertThat(recastedDummyListOfInterfaces).isNotNull();
    assertThat(recastedDummyListOfInterfaces.iTests).isEqualTo(iTests);
    assertThat(recastedDummyListOfInterfaces.iTests.get(0)).isEqualTo(iTest);
    assertThat(recastedDummyListOfInterfaces.iTests.get(1)).isEqualTo(iTest1);
    assertThat(recastedDummyListOfInterfaces.iTests.get(2)).isEqualTo(iTest2);
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyListOfInterfaces {
    private List<ITest> iTests;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfMapOFInterfaces() {
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");
    List<Map<String, ITest>> iTestsMap = ImmutableList.of(
        ImmutableMap.of("itest", iTest), ImmutableMap.of("itest1", iTest1), ImmutableMap.of("itest2", iTest2));

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfMapOfInterfaces.class));

    DummyListOfMapOfInterfaces dummyListOfInterfaces =
        DummyListOfMapOfInterfaces.builder().iTestsMap(iTestsMap).build();

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY, DummyListOfMapOfInterfaces.class.getName(),
        "iTestsMap",
        ImmutableList.of(
            ImmutableMap.of("itest", ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType")),
            ImmutableMap.of("itest1", ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType1")),
            ImmutableMap.of("itest2", ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType2"))));

    Map<String, Object> document = recast.toMap(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfMapOfInterfaces recasted = recast.fromMap(document, DummyListOfMapOfInterfaces.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted.iTestsMap).isEqualTo(iTestsMap);
    for (Map<String, ITest> testMap : recasted.iTestsMap) {
      assertThat(testMap.values().stream().allMatch(v -> v instanceof ITestImpl)).isTrue();
    }
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyListOfMapOfInterfaces {
    private List<Map<String, ITest>> iTestsMap;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyValueListMap() {
    ArrayList<String> strings = new ArrayList<>();
    strings.add("status");
    Map<String, List<String>> map = new HashMap<>();
    map.put("Test", strings);
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyStringKeyValueListMap.class));
    DummyStringKeyValueListMap stringKeyMap = DummyStringKeyValueListMap.builder().map(map).build();

    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyStringKeyValueListMap.class.getName());
    assertThat((Map<String, Object>) document.get("map")).isEqualTo(ImmutableMap.of("Test", strings));

    DummyStringKeyValueListMap recastedDummyMap = recast.fromMap(document, DummyStringKeyValueListMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(recastedDummyMap.map.size()).isEqualTo(map.size());
    recastedDummyMap.map.forEach((key, value) -> assertThat(map.get(key).containsAll(value)));
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyStringKeyValueListMap {
    private Map<String, List<String>> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyValueMap() {
    Map<String, Map<String, String>> map = new HashMap<>();
    map.put("Test", ImmutableMap.of("status", "Success"));
    map.put("Test1", ImmutableMap.of("status", "Success"));
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyStringKeyValueMap.class));
    DummyStringKeyValueMap stringKeyMap = DummyStringKeyValueMap.builder().map(map).build();

    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyStringKeyValueMap.class.getName());
    assertThat((Map<String, Object>) document.get("map"))
        .isEqualTo(ImmutableMap.of(
            "Test", ImmutableMap.of("status", "Success"), "Test1", ImmutableMap.of("status", "Success")));

    DummyStringKeyValueMap recastedDummyMap = recast.fromMap(document, DummyStringKeyValueMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(recastedDummyMap.map).isEqualTo(map);
  }

  @Builder
  @AllArgsConstructor
  private static class DummyStringKeyValueMap {
    private Map<String, Map<String, String>> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyListOfListOfListOfInterfaceValueMap() {
    List<Integer> firstList = Arrays.asList(1, 2, 3, 4);
    List<Integer> secondList = Arrays.asList(1, 2, 3, 4, 5);

    Map<String, List<Integer>> complexMap = new HashMap<>();
    complexMap.put("four", firstList);
    complexMap.put("five", secondList);

    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl1("someType1", complexMap);

    Map<String, List<List<List<ITest>>>> map = new HashMap<>();
    map.put("Test", ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest))));
    map.put("Test1", ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest1))));
    DummyKeyStringValueListOfListOfListOfInterfaceMap stringKeyMap =
        DummyKeyStringValueListOfListOfListOfInterfaceMap.builder().map(map).build();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyKeyStringValueListOfListOfListOfInterfaceMap.class));

    Map<String, Object> expected = ImmutableMap.of(RECAST_KEY,
        DummyKeyStringValueListOfListOfListOfInterfaceMap.class.getName(), "map",
        ImmutableMap.of("Test",
            ImmutableList.of(ImmutableList.of(
                ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "someType")))),
            "Test1",
            ImmutableList.of(ImmutableList.of(ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl1.class.getName(),
                "type1", "someType1", "map", ImmutableMap.of("four", firstList, "five", secondList)))))));

    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummyKeyStringValueListOfListOfListOfInterfaceMap recastedListOfListEtc =
        recast.fromMap(document, DummyKeyStringValueListOfListOfListOfInterfaceMap.class);
    assertThat(recastedListOfListEtc).isNotNull();
    assertThat(recastedListOfListEtc.map).isNotEmpty();
    assertThat(
        recastedListOfListEtc.map.get("Test").equals(ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest)))))
        .isTrue();
    assertThat(
        recastedListOfListEtc.map.get("Test1").equals(ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest1)))))
        .isTrue();
  }

  @Builder
  @AllArgsConstructor
  private static class DummyKeyStringValueListOfListOfListOfInterfaceMap {
    private Map<String, List<List<List<ITest>>>> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyInterfacesValueMap() {
    List<Integer> firstList = Arrays.asList(1, 2, 3, 4);
    List<Integer> secondList = Arrays.asList(1, 2, 3, 4, 5);
    Map<String, List<Integer>> complexMap = new HashMap<>();
    complexMap.put("four", firstList);
    complexMap.put("five", secondList);

    Map<String, ITest> map = new HashMap<>();
    map.put("Test", new ITestImpl("test"));
    map.put("Test1", new ITestImpl1("test1", complexMap));
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyStringKeyValueInterfaceMap.class));
    DummyStringKeyValueInterfaceMap stringKeyMap = DummyStringKeyValueInterfaceMap.builder().map(map).build();

    Map<String, Object> expected = ImmutableMap.of(RECAST_KEY, DummyStringKeyValueInterfaceMap.class.getName(), "map",
        ImmutableMap.of("Test", ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "test"), "Test1",
            ImmutableMap.of(RECAST_KEY, ITestImpl1.class.getName(), "type1", "test1", "map",
                ImmutableMap.of("four", firstList, "five", secondList))));

    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummyStringKeyValueInterfaceMap recastedDummyMap = recast.fromMap(document, DummyStringKeyValueInterfaceMap.class);
    assertThat(recastedDummyMap).isNotNull();
    recastedDummyMap.map.forEach((key, value) -> assertThat(map.get(key).equals(value)));
  }

  @Builder
  @AllArgsConstructor
  private static class DummyStringKeyValueInterfaceMap {
    private Map<String, ITest> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithStringKeyMapOfMapOfListOfInterfaceValueMap() {
    List<Integer> firstList = Arrays.asList(1, 2, 3, 4);
    List<Integer> secondList = Arrays.asList(1, 2, 3, 4, 5);
    Map<String, List<Integer>> complexMap = new HashMap<>();
    complexMap.put("four", firstList);
    complexMap.put("five", secondList);

    Map<String, Map<String, List<ITest>>> test =
        ImmutableMap.of("TestMap02", ImmutableMap.of("TestMap03-list", ImmutableList.of(new ITestImpl("test0123"))));
    Map<String, Map<String, List<ITest>>> test1 = ImmutableMap.of(
        "TestMap12", ImmutableMap.of("TestMap13-list", ImmutableList.of(new ITestImpl1("test1", complexMap))));
    Map<String, Map<String, Map<String, List<ITest>>>> complexMapOfMapEtc = new HashMap<>();
    complexMapOfMapEtc.put("Test0", test);
    complexMapOfMapEtc.put("Test1", test1);

    DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap stringKeyMap =
        DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.builder().complexMapOfMapEtc(complexMapOfMapEtc).build();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class));

    Map<String, Object> expectedDoc = ImmutableMap.of(RECAST_KEY,
        DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class.getName(), "complexMapOfMapEtc",
        ImmutableMap.of("Test0",
            ImmutableMap.of("TestMap02",
                ImmutableMap.of("TestMap03-list",
                    ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl.class.getName(), "type", "test0123")))),
            "Test1",
            ImmutableMap.of("TestMap12",
                ImmutableMap.of("TestMap13-list",
                    ImmutableList.of(ImmutableMap.of(RECAST_KEY, ITestImpl1.class.getName(), "type1", "test1", "map",
                        ImmutableMap.of("four", firstList, "five", secondList)))))));

    Map<String, Object> document = recast.toMap(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expectedDoc);

    DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap recastedDummyMap =
        recast.fromMap(document, DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class);
    assertThat(recastedDummyMap).isNotNull();
    recastedDummyMap.complexMapOfMapEtc.forEach((key, value) -> assertThat(complexMapOfMapEtc.get(key).equals(value)));
  }

  @Builder
  @AllArgsConstructor
  private static class DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap {
    private Map<String, Map<String, Map<String, List<ITest>>>> complexMapOfMapEtc;
  }

  private interface ITest {
    String getType();
  }

  private interface ITestChildWithComplexMap extends ITest {
    Map<String, List<Integer>> getComplexMap();
  }

  @Builder
  @EqualsAndHashCode
  private static class ITestImpl implements ITest {
    private final String type;

    @Override
    public String getType() {
      return type;
    }
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ITestImpl1 implements ITestChildWithComplexMap {
    private String type1;
    private Map<String, List<Integer>> map;

    @Override
    public String getType() {
      return type1;
    }

    @Override
    public Map<String, List<Integer>> getComplexMap() {
      return map;
    }
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ITestImpl2 implements ITest {
    private String type2;
    @Override
    public String getType() {
      return type2;
    }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithInnerClass() {
    final String id = "dgrr4kg02-24ger40bdf-4";
    final String name = "sgnjdfg";
    final Integer age = 21;
    DummyWithInnerClass.User user = DummyWithInnerClass.User.builder().name(name).age(age).build();
    Recast recast = new Recast(recaster, ImmutableSet.of(DummyWithInnerClass.class));
    DummyWithInnerClass dummyWithInnerClass = DummyWithInnerClass.builder().id(id).user(user).build();

    Map<String, Object> document = recast.toMap(dummyWithInnerClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyWithInnerClass.class.getName());
    assertThat(document.get("id")).isEqualTo(id);
    Map<String, Object> userDocument = (Map<String, Object>) document.get("user");
    assertThat(userDocument).isNotEmpty();
    assertThat(userDocument.get(RECAST_KEY)).isEqualTo(DummyWithInnerClass.User.class.getName());
    assertThat(userDocument.get("name")).isEqualTo(name);
    assertThat(userDocument.get("age")).isEqualTo(age);

    DummyWithInnerClass recastedDummyWithInnerClass = recast.fromMap(document, DummyWithInnerClass.class);
    assertThat(recastedDummyWithInnerClass).isNotNull();
    assertThat(recastedDummyWithInnerClass.id).isEqualTo(id);
    assertThat(recastedDummyWithInnerClass.user).isEqualTo(user);
  }

  @Builder
  @AllArgsConstructor
  private static class DummyWithInnerClass {
    private String id;
    private User user;

    @Builder
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class User {
      private String name;
      private Integer age;
    }
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedField() {
    DummyParameterized<Boolean> parameterized = DummyParameterized.<Boolean>builder().expression(true).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(true);

    DummyParameterized<Boolean> recasted = recast.fromMap(document, DummyParameterized.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsList() {
    DummyParameterized<List<Boolean>> parameterized =
        DummyParameterized.<List<Boolean>>builder().expression(Collections.singletonList(true)).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(ImmutableList.of(true));

    DummyParameterized<List<Boolean>> recasted = recast.fromMap(document, DummyParameterized.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsListOf() {
    DummyWithInnerClass.User user = new DummyWithInnerClass.User("name", 23);
    DummyParameterized<List<DummyWithInnerClass.User>> parameterized =
        DummyParameterized.<List<DummyWithInnerClass.User>>builder()
            .expression(Collections.singletonList(user))
            .build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression"))
        .isEqualTo(ImmutableList.of(
            ImmutableMap.of(RECAST_KEY, DummyWithInnerClass.User.class.getName(), "name", "name", "age", 23)));

    DummyParameterized<List<DummyWithInnerClass.User>> recasted = recast.fromMap(document, DummyParameterized.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsMap() {
    DummyParameterized<Map<String, String>> parameterized =
        DummyParameterized.<Map<String, String>>builder().expression(Collections.singletonMap("key", "value")).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(ImmutableMap.of("key", "value"));

    DummyParameterized<Map<String, Integer>> recasted = recast.fromMap(document, DummyParameterized.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsClassField() {
    DummyParameterized<Map<String, String>> parameterized =
        DummyParameterized.<Map<String, String>>builder().expression(Collections.singletonMap("key", "value")).build();
    DummyParameterizedInside dummyParameterizedInside = DummyParameterizedInside.builder().map(parameterized).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(dummyParameterizedInside);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterizedInside.class.getName());
    assertThat(document.get("map"))
        .isEqualTo(ImmutableMap.of(
            RECAST_KEY, DummyParameterized.class.getName(), "expression", ImmutableMap.of("key", "value")));

    DummyParameterizedInside recasted = recast.fromMap(document, DummyParameterizedInside.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(dummyParameterizedInside);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsClass() {
    DummyWithInnerClass.User user = new DummyWithInnerClass.User("name", 23);
    DummyParameterized<DummyWithInnerClass.User> parameterized =
        DummyParameterized.<DummyWithInnerClass.User>builder().expression(user).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression"))
        .isEqualTo(ImmutableMap.of(RECAST_KEY, DummyWithInnerClass.User.class.getName(), "name", "name", "age", 23));

    DummyParameterized<DummyWithInnerClass.User> recasted = recast.fromMap(document, DummyParameterized.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParameterizedFieldAsClassWrapper() {
    DummyWithInnerClass.User user = new DummyWithInnerClass.User("name", 23);
    DummyParameterized<DummyWithInnerClass.User> parameterized =
        DummyParameterized.<DummyWithInnerClass.User>builder().expression(user).build();
    DummyPrameterizedWrapper wrapper = DummyPrameterizedWrapper.builder().user(parameterized).build();
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(wrapper);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyPrameterizedWrapper.class.getName());

    // assertThat(document.get("expression")).isEqualTo(true);

    DummyPrameterizedWrapper recasted = recast.fromMap(document, DummyPrameterizedWrapper.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(wrapper);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParamField() {
    DummyParam parameterized = new DummyParam(true, false);
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParam.class.getName());
    assertThat(document.get("a")).isEqualTo(false);
    assertThat(document.get("expression")).isEqualTo(true);

    DummyParam recasted = recast.fromMap(document, DummyParam.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(parameterized);
  }

  private static class DummyParam extends DummyParameterized<Boolean> {
    @Setter private Boolean a;

    DummyParam(Boolean expression, Boolean a) {
      super(expression);
      this.a = a;
    }
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyParameterized<T> {
    @Setter T expression;
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyPrameterizedWrapper {
    DummyParameterized<DummyWithInnerClass.User> user;
  }

  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyParameterizedInside {
    @Setter DummyParameterized<Map<String, String>> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterClassExtendMap() {
    DummyParameterized<Map<String, String>> parameterized =
        DummyParameterized.<Map<String, String>>builder().expression(Collections.singletonMap("key", "value")).build();
    DummyParameterizedInside dummyParameterizedInside = DummyParameterizedInside.builder().map(parameterized).build();
    DummyExtendsMapClass dummyExtendsMapClass = new DummyExtendsMapClass();
    dummyExtendsMapClass.put("Key", dummyParameterizedInside);
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Map<String, Object> document = recast.toMap(dummyExtendsMapClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyExtendsMapClass.class.getName());
    assertThat(document.get("Key"))
        .isEqualTo(ImmutableMap.of(RECAST_KEY, DummyParameterizedInside.class.getName(), "map",
            ImmutableMap.of(
                RECAST_KEY, DummyParameterized.class.getName(), "expression", ImmutableMap.of("key", "value"))));

    DummyExtendsMapClass recasted = recast.fromMap(document, DummyExtendsMapClass.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(dummyExtendsMapClass);
  }

  @EqualsAndHashCode(callSuper = false)
  private static class DummyExtendsMapClass extends HashMap<String, Object> {}

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterClassExtendsList() {
    DummyParameterized<Map<String, String>> parameterized =
        DummyParameterized.<Map<String, String>>builder().expression(Collections.singletonMap("key", "value")).build();
    DummyParameterizedInside dummyParameterizedInside = DummyParameterizedInside.builder().map(parameterized).build();
    DummyParameterized<Map<String, String>> parameterized1 = DummyParameterized.<Map<String, String>>builder()
                                                                 .expression(Collections.singletonMap("key1", "value1"))
                                                                 .build();
    DummyParameterizedInside dummyParameterizedInside1 = DummyParameterizedInside.builder().map(parameterized1).build();

    DummyExtendsListClass dummyExtendsMapClass = new DummyExtendsListClass();
    dummyExtendsMapClass.add(dummyParameterizedInside);
    dummyExtendsMapClass.add(dummyParameterizedInside1);
    Recast recast = new Recast(recaster, ImmutableSet.of());

    DummyExtendsListClass expected = new DummyExtendsListClass();
    expected.add(ImmutableMap.of(RECAST_KEY, DummyParameterizedInside.class.getName(), "map",
        ImmutableMap.of(
            RECAST_KEY, DummyParameterized.class.getName(), "expression", ImmutableMap.of("key", "value"))));
    expected.add(ImmutableMap.of(RECAST_KEY, DummyParameterizedInside.class.getName(), "map",
        ImmutableMap.of(
            "__recast", DummyParameterized.class.getName(), "expression", ImmutableMap.of("key1", "value1"))));

    Map<String, Object> document = recast.toMap(dummyExtendsMapClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyExtendsListClass.class.getName());
    assertThat(document.get(Recaster.ENCODED_VALUE)).isEqualTo(expected);

    DummyExtendsListClass recasted = recast.fromMap(document, DummyExtendsListClass.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted.get(0)).isInstanceOf(DummyParameterizedInside.class);
    assertThat(recasted).isEqualTo(dummyExtendsMapClass);
  }

  @EqualsAndHashCode(callSuper = false)
  private static class DummyExtendsListClass extends ArrayList<Object> {}
}
