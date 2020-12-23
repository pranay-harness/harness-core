package io.harness.core;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.RecasterTestBase;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecastComplexValuesTest extends RecasterTestBase {
  private Recaster recaster;

  @Before
  public void setup() {
    recaster = new Recaster();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithInterface() {
    ITest iTest = new ITestImpl("someType");
    Recast recast = new Recast(recaster, ImmutableSet.of(ITest.class));

    Document document = recast.toDocument(iTest);
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

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document.get("iTests")).isEqualTo(iTests);

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
    assertThat((Document) document.get("map")).isEqualTo(Document.parse(gson.toJson(map)));

    DummyStringKeyValueListMap recastedDummyMap = recast.fromDocument(document, DummyStringKeyValueListMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(recastedDummyMap.map.size()).isEqualTo(map.size());
    recastedDummyMap.map.forEach((key, value) -> assertThat(map.get(key).containsAll(value)));
  }

  @Builder
  @NoArgsConstructor
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
    assertThat((Document) document.get("map")).isEqualTo(Document.parse(gson.toJson(map)));

    DummyStringKeyValueMap recastedDummyMap = recast.fromDocument(document, DummyStringKeyValueMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(recastedDummyMap.map).isEqualTo(map);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyStringKeyValueMap {
    private Map<String, Map<String, String>> map;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithListOfInterfaceWithDifferentImplementations() {
    Map<String, List<Integer>> iTest1map = new HashMap<>();
    iTest1map.put("four", Arrays.asList(1, 2, 3, 4));
    iTest1map.put("five", Arrays.asList(1, 2, 3, 4, 5));

    ITest iTest = new ITestImpl("someType");
    ITest iTest1 = new ITestImpl1("someType1", iTest1map);
    ITest iTest2 = new ITestImpl2("someType2");
    List<ITest> iTests = Arrays.asList(iTest, iTest1, iTest2);
    DummyListOfInterfaces dummyListOfInterfaces = DummyListOfInterfaces.builder().iTests(iTests).build();

    Recast recast = new Recast(recaster, ImmutableSet.of(DummyListOfInterfaces.class));

    Document document = recast.toDocument(dummyListOfInterfaces);
    assertThat(document.get("iTests")).isEqualTo(iTests);

    DummyListOfInterfaces recastedDummyListOfInterfaces = recast.fromDocument(document, DummyListOfInterfaces.class);
    assertThat(recastedDummyListOfInterfaces).isNotNull();
    assertThat(recastedDummyListOfInterfaces.iTests).isEqualTo(iTests);
    assertThat(recastedDummyListOfInterfaces.iTests.get(0)).isEqualTo(iTest);
    assertThat(recastedDummyListOfInterfaces.iTests.get(1)).isEqualTo(iTest1);
    assertThat(recastedDummyListOfInterfaces.iTests.get(2)).isEqualTo(iTest2);
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
            .append(
                "__recast", "io.harness.core.RecastComplexValuesTest$DummyKeyStringValueListOfListOfListOfInterfaceMap")
            .append("map",
                new Document()
                    .append("Test", ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest))))
                    .append("Test1", ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest1)))));
    ;

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expected);

    DummyKeyStringValueListOfListOfListOfInterfaceMap recastedListOfListEtc =
        recast.fromDocument(document, DummyKeyStringValueListOfListOfListOfInterfaceMap.class);
    assertThat(recastedListOfListEtc).isNotNull();
    assertThat(recastedListOfListEtc.map).isNotEmpty();
    assertThat(
        recastedListOfListEtc.map.get("Test").contains(ImmutableList.of(ImmutableList.of(ImmutableList.of(iTest)))))
        .isTrue();
  }

  @Builder
  @NoArgsConstructor
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
            .append("Test",
                new Document()
                    .append("__recast", "io.harness.core.RecastComplexValuesTest$ITestImpl")
                    .append("type", "test"))
            .append("Test1",
                new Document()
                    .append("__recast", "io.harness.core.RecastComplexValuesTest$ITestImpl1")
                    .append("type1", "test1")
                    .append("map", new Document().append("four", firstList).append("five", secondList)));

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat((Document) document.get("map")).isEqualTo(expected);

    DummyStringKeyValueInterfaceMap recastedDummyMap =
        recast.fromDocument(document, DummyStringKeyValueInterfaceMap.class);
    assertThat(recastedDummyMap).isNotNull();
    recastedDummyMap.map.forEach((key, value) -> assertThat(map.get(key).equals(value)));
  }

  @Builder
  @NoArgsConstructor
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
            .append("__recast",
                "io.harness.core.RecastComplexValuesTest$DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap")
            .append("complexMapOfMapEtc",
                new Document()
                    .append("Test0",
                        ImmutableMap.of("TestMap02",
                            ImmutableMap.of("TestMap03-list",
                                ImmutableList.of(
                                    new Document()
                                        .append("__recast", "io.harness.core.RecastComplexValuesTest$ITestImpl")
                                        .append("type", "test0123")))))
                    .append("Test1",
                        ImmutableMap.of("TestMap12",
                            ImmutableMap.of("TestMap13-list",
                                ImmutableList.of(
                                    new Document()
                                        .append("__recast", "io.harness.core.RecastComplexValuesTest$ITestImpl1")
                                        .append("type1", "test1")
                                        .append("map",
                                            new Document().append("four", firstList).append("five", secondList)))))));

    Document document = recast.toDocument(stringKeyMap);
    assertThat(document).isNotEmpty();
    assertThat(document).isEqualTo(expectedDoc);

    DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap recastedDummyMap =
        recast.fromDocument(document, DummyKeyStringValueMapOfMapOfMapOfListInterfaceMap.class);
    assertThat(recastedDummyMap).isNotNull();
    assertThat(
        recastedDummyMap.complexMapOfMapEtc.get("Test0").get("TestMap02").get("TestMap03-list").get(0) instanceof ITest)
        .isTrue();
    recastedDummyMap.complexMapOfMapEtc.forEach((key, value) -> assertThat(complexMapOfMapEtc.get(key).equals(value)));
  }

  @Builder
  @NoArgsConstructor
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
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ITestImpl implements ITest {
    private String type;

    @Override
    public String getType() {
      return type;
    }
  }

  @Builder
  @NoArgsConstructor
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
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ITestImpl2 implements ITest {
    private String type2;
    @Override
    public String getType() {
      return type2;
    }
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class DummyListOfInterfaces {
    private List<ITest> iTests;
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
    assertThat(document.get("__recast")).isEqualTo(DummyWithInnerClass.class.getName());
    assertThat(document.get("id")).isEqualTo(id);
    Document userDocument = (Document) document.get("user");
    assertThat(userDocument).isNotEmpty();
    assertThat(userDocument.get("name")).isEqualTo(name);
    assertThat(userDocument.get("age")).isEqualTo(age);

    DummyWithInnerClass recastedDummyWithInnerClass = recast.fromDocument(document, DummyWithInnerClass.class);
    assertThat(recastedDummyWithInnerClass).isNotNull();
    assertThat(recastedDummyWithInnerClass.id).isEqualTo(id);
    assertThat(recastedDummyWithInnerClass.user).isEqualTo(user);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class DummyWithInnerClass {
    private String id;
    private DummyWithInnerClass.User user;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class User {
      private String name;
      private Integer age;
    }
  }
}
