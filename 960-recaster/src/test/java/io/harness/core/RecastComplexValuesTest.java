package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.RecasterTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exceptions.RecasterException;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
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
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecastComplexValuesTest extends RecasterTestBase {
  private static final String RECAST_KEY = Recaster.RECAST_CLASS_KEY;
  private Recaster recaster;

  @Before
  public void setup() {
    recaster = new Recaster();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterWithSimpleList() {
    final List<Integer> list = Arrays.asList(1, 2, 3);

    Recast recast = new Recast(recaster, ImmutableSet.of(DummySimpleList.class));
    DummySimpleList dummyList = DummySimpleList.builder().list(list).build();
    Document document = recast.toDocument(dummyList);

    assertThat(document).isNotEmpty();
    assertThat(document).isNotEmpty();
    assertThat(document.get("list")).isEqualTo(list);

    DummySimpleList recastedSimpleList = recast.fromDocument(document, DummySimpleList.class);
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

    Document expected = new Document().append(RECAST_KEY, DummySimpleSet.class.getName()).append("set", set);

    Document document = recast.toDocument(dummySet);

    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummySimpleSet recastedSimpleSet = recast.fromDocument(document, DummySimpleSet.class);
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

    Document document = recast.toDocument(iTest);
    assertThat(document.get(RECAST_KEY)).isEqualTo(ITestImpl.class.getName());
    assertThat(document.get("type")).isEqualTo("someType");

    ITest recastedITest = recast.fromDocument(document, ITest.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, "io.harness.core.RecastComplexValuesTest$DummyListOfInterfaces")
            .append("iTests",
                ImmutableList.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2")));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromDocument(document, DummyListOfInterfaces.class);
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

    Document document =
        new Document()
            .append(RECAST_KEY, "io.harness.core.RecastComplexValuesTest$DummyListOfInterfaces")
            .append("iTests",
                ImmutableList.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2")));

    Recast recast = new Recast(recaster, ImmutableSet.of());

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromDocument(document, DummyListOfInterfaces.class);
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
    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl("someType1");
    ITest iTest2 = new ITestImpl("someType2");

    Document document =
        new Document()
            .append(RECAST_KEY, DummyListOfSetOfInterfaces.class.getName())
            .append("list",
                ImmutableList.of(ImmutableSet.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2"))));

    Recast recast = new Recast(recaster, ImmutableSet.of());

    assertThatThrownBy(() -> recast.fromDocument(document, DummyListOfInterfaces.class))
        .isInstanceOf(RecasterException.class)
        .hasMessageContaining(
            "io.harness.core.RecastComplexValuesTest$DummyListOfSetOfInterfaces class cannot be mapped to io.harness.core.RecastComplexValuesTest$DummyListOfInterfaces");
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyListOfSetOfInterfaces.class.getName())
            .append("list",
                ImmutableList.of(ImmutableSet.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2"))));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfSetOfInterfaces recasted = recast.fromDocument(document, DummyListOfSetOfInterfaces.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyListOfSetOfListOfSetInterfaces.class.getName())
            .append("list",
                ImmutableList.of(ImmutableSet.of(ImmutableList.of(ImmutableSet.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2"))))));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfSetOfListOfSetInterfaces recasted =
        recast.fromDocument(document, DummyListOfSetOfListOfSetInterfaces.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyListStringKeyOfSetOfListOfSetInterfacesMap.class.getName())
            .append("damnMap",
                new Document("WOOW",
                    ImmutableList.of(ImmutableSet.of(ImmutableList.of(ImmutableSet.of(
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1"),
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2")))))));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListStringKeyOfSetOfListOfSetInterfacesMap recasted =
        recast.fromDocument(document, DummyListStringKeyOfSetOfListOfSetInterfacesMap.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyListOfInterfaces.class.getName())
            .append("iTests",
                ImmutableList.of(
                    new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType"),
                    new Document()
                        .append(RECAST_KEY, ITestImpl1.class.getName())
                        .append("type1", "someType1")
                        .append("map", new Document().append("four", fourArray).append("five", fiveArray)),
                    new Document().append(RECAST_KEY, ITestImpl2.class.getName()).append("type2", "someType2")));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromDocument(document, DummyListOfInterfaces.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyListOfMapOfInterfaces.class.getName())
            .append("iTestsMap",
                ImmutableList.of(
                    new Document("itest",
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType")),
                    new Document("itest1",
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType1")),
                    new Document("itest2",
                        new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType2"))));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document).isEqualTo(expectedDoc);

    DummyListOfMapOfInterfaces recasted = recast.fromDocument(document, DummyListOfMapOfInterfaces.class);
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

    Gson gson = new Gson();
    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyStringKeyValueListMap.class.getName());
    assertThat((Document) document.get("map")).isEqualTo(Document.parse(gson.toJson(map)));

    DummyStringKeyValueListMap recastedDummyMap = recast.fromDocument(document, DummyStringKeyValueListMap.class);
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

    Gson gson = new Gson();
    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyStringKeyValueMap.class.getName());
    assertThat((Document) document.get("map")).isEqualTo(Document.parse(gson.toJson(map)));

    DummyStringKeyValueMap recastedDummyMap = recast.fromDocument(document, DummyStringKeyValueMap.class);
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

    Document expected =
        new Document()
            .append(RECAST_KEY, DummyKeyStringValueListOfListOfListOfInterfaceMap.class.getName())
            .append("map",
                new Document()
                    .append("Test",
                        ImmutableList.of(ImmutableList.of(ImmutableList.of(
                            new Document().append(RECAST_KEY, ITestImpl.class.getName()).append("type", "someType")))))
                    .append("Test1",
                        ImmutableList.of(ImmutableList.of(ImmutableList.of(
                            new Document()
                                .append(RECAST_KEY, ITestImpl1.class.getName())
                                .append("type1", "someType1")
                                .append(
                                    "map", new Document().append("four", firstList).append("five", secondList)))))));

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummyKeyStringValueListOfListOfListOfInterfaceMap recastedListOfListEtc =
        recast.fromDocument(document, DummyKeyStringValueListOfListOfListOfInterfaceMap.class);
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

    Document expected =
        new Document()
            .append(RECAST_KEY, DummyStringKeyValueInterfaceMap.class.getName())
            .append("map",
                new Document()
                    .append("Test",
                        new Document()
                            .append(RECAST_KEY, "io.harness.core.RecastComplexValuesTest$ITestImpl")
                            .append("type", "test"))
                    .append("Test1",
                        new Document()
                            .append(RECAST_KEY, "io.harness.core.RecastComplexValuesTest$ITestImpl1")
                            .append("type1", "test1")
                            .append("map", new Document().append("four", firstList).append("five", secondList))));

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummyStringKeyValueInterfaceMap recastedDummyMap =
        recast.fromDocument(document, DummyStringKeyValueInterfaceMap.class);
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

    Document expectedDoc =
        new Document()
            .append(RECAST_KEY, DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class.getName())
            .append("complexMapOfMapEtc",
                new Document()
                    .append("Test0",
                        new Document("TestMap02",
                            new Document("TestMap03-list",
                                ImmutableList.of(new Document()
                                                     .append(RECAST_KEY, ITestImpl.class.getName())
                                                     .append("type", "test0123")))))
                    .append("Test1",
                        new Document("TestMap12",
                            new Document("TestMap13-list",
                                ImmutableList.of(
                                    new Document()
                                        .append(RECAST_KEY, ITestImpl1.class.getName())
                                        .append("type1", "test1")
                                        .append("map",
                                            new Document().append("four", firstList).append("five", secondList)))))));

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expectedDoc);

    DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap recastedDummyMap =
        recast.fromDocument(document, DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class);
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

    Document document = recast.toDocument(dummyWithInnerClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyWithInnerClass.class.getName());
    assertThat(document.get("id")).isEqualTo(id);
    Document userDocument = (Document) document.get("user");
    assertThat(userDocument).isNotEmpty();
    assertThat(userDocument.get(RECAST_KEY)).isEqualTo(DummyWithInnerClass.User.class.getName());
    assertThat(userDocument.get("name")).isEqualTo(name);
    assertThat(userDocument.get("age")).isEqualTo(age);

    DummyWithInnerClass recastedDummyWithInnerClass = recast.fromDocument(document, DummyWithInnerClass.class);
    assertThat(recastedDummyWithInnerClass).isNotNull();
    assertThat(recastedDummyWithInnerClass.id).isEqualTo(id);
    assertThat(recastedDummyWithInnerClass.user).isEqualTo(user);
  }

  @Builder
  @AllArgsConstructor
  private static class DummyWithInnerClass {
    private String id;
    private DummyWithInnerClass.User user;

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

    Document document = recast.toDocument(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(true);

    DummyParameterized<Boolean> recasted = recast.fromDocument(document, DummyParameterized.class);
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

    Document document = recast.toDocument(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(ImmutableList.of(true));

    DummyParameterized<List<Boolean>> recasted = recast.fromDocument(document, DummyParameterized.class);
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

    Document document = recast.toDocument(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterized.class.getName());
    assertThat(document.get("expression")).isEqualTo(new Document().append("key", "value"));

    DummyParameterized<Map<String, Integer>> recasted = recast.fromDocument(document, DummyParameterized.class);
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

    Document document = recast.toDocument(dummyParameterizedInside);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParameterizedInside.class.getName());
    assertThat(document.get("map"))
        .isEqualTo(new Document()
                       .append(RECAST_KEY, DummyParameterized.class.getName())
                       .append("expression", new Document().append("key", "value")));

    DummyParameterizedInside recasted = recast.fromDocument(document, DummyParameterizedInside.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(dummyParameterizedInside);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecasterParamField() {
    DummyParam parameterized = new DummyParam(true, false);
    Recast recast = new Recast(recaster, ImmutableSet.of());

    Document document = recast.toDocument(parameterized);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyParam.class.getName());
    assertThat(document.get("a")).isEqualTo(false);
    assertThat(document.get("expression")).isEqualTo(true);

    DummyParam recasted = recast.fromDocument(document, DummyParam.class);
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

    Document document = recast.toDocument(dummyExtendsMapClass);
    assertThat(document).isNotEmpty();
    assertThat(document.get(RECAST_KEY)).isEqualTo(DummyExtendsMapClass.class.getName());
    assertThat(document.get("Key"))
        .isEqualTo(new Document()
                       .append(RECAST_KEY, DummyParameterizedInside.class.getName())
                       .append("map",
                           new Document()
                               .append(RECAST_KEY, DummyParameterized.class.getName())
                               .append("expression", new Document().append("key", "value"))));

    DummyExtendsMapClass recasted = recast.fromDocument(document, DummyExtendsMapClass.class);
    assertThat(recasted).isNotNull();
    assertThat(recasted).isEqualTo(dummyExtendsMapClass);
  }

  @EqualsAndHashCode(callSuper = false)
  private static class DummyExtendsMapClass extends HashMap<String, Object> {}
}
