package io.harness.pms.serializer.recaster;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtilsTest.DummyOutput.DummyOutputKeys;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtilsTest.DummyOutput.Pair;
import io.harness.rule.Owner;
import lombok.Singular;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class RecastOrchestrationUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestToMapFromEntity() {
    Map<String, Object> kvMap = ImmutableMap.of("a.b", "c", "d.e", ImmutableMap.of("f.g", "h.i"));
    DummyOutput o = new DummyOutput("testString", kvMap, new Pair(ImmutableMap.of("j.k", "l.m")));
    Map<String, Object> oMap = RecastOrchestrationUtils.toMap(o);
    assertThat(oMap.keySet()).contains(DummyOutputKeys.test, DummyOutputKeys.keyValuePairs, DummyOutputKeys.pair);
    assertThat(oMap.get(DummyOutputKeys.keyValuePairs)).isInstanceOf(Map.class);
    assertThat(oMap.get(DummyOutputKeys.keyValuePairs)).isNotInstanceOf(Document.class);
    assertThat(((Map<String, Object>) oMap.get(DummyOutputKeys.keyValuePairs)).keySet()).containsExactly("a.b", "d.e");

    assertThat(((Map<String, Object>) ((Map<String, Object>) oMap.get(DummyOutputKeys.pair)).get("keyVal")).keySet())
        .contains("j.k");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestToMapFromJson() {
    Map<String, Object> kvMap = ImmutableMap.of("a.b", "c", "d.e", ImmutableMap.of("f.g", "h.i"));
    DummyOutput o = new DummyOutput("testString", kvMap, new Pair(ImmutableMap.of("j.k", "l.m")));
    String json = RecastOrchestrationUtils.toDocumentJson(o);
    Map<String, Object> oMap = RecastOrchestrationUtils.toMapFromJson(json);
    assertThat(oMap.keySet()).contains(DummyOutputKeys.test, DummyOutputKeys.keyValuePairs, DummyOutputKeys.pair);
    assertThat(oMap.get(DummyOutputKeys.keyValuePairs)).isInstanceOf(Map.class);
    assertThat(oMap.get(DummyOutputKeys.keyValuePairs)).isNotInstanceOf(Document.class);
    assertThat(((Map<String, Object>) oMap.get(DummyOutputKeys.keyValuePairs)).keySet()).containsExactly("a.b", "d.e");

    assertThat(((Map<String, Object>) ((Map<String, Object>) oMap.get(DummyOutputKeys.pair)).get("keyVal")).keySet())
        .contains("j.k");
  }

  public static class DummyOutput {
    String test;
    @Singular Map<String, Object> keyValuePairs;
    Pair pair;

    public DummyOutput(String test, Map<String, Object> keyValuePairs, Pair pair) {
      this.test = test;
      this.keyValuePairs = keyValuePairs;
      this.pair = pair;
    }

    public static final class Pair {
      @Singular Map<String, Object> keyVal;

      public Pair(Map<String, Object> keyVal) {
        this.keyVal = keyVal;
      }
    }

    public static final class DummyOutputKeys {
      public final static String test = "test";
      public final static String keyValuePairs = "keyValuePairs";
      public final static String pair = "pair";
    }
  }
}
