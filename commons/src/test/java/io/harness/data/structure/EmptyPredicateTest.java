package io.harness.data.structure;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.CategoryTest;
import io.harness.category.FastUnitTests;
import io.harness.data.structure.EmptyPredicate.IsEmpty;
import lombok.Builder;
import lombok.Getter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmptyPredicateTest extends CategoryTest {
  @Builder
  @Getter
  static class Custom implements IsEmpty {
    boolean empty;
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNullIsEmpty() {
    int[] array = null;
    assertTrue(isEmpty(array));

    Custom custom = null;
    assertTrue(isEmpty(custom));

    List list = null;
    assertTrue(isEmpty(list));

    String string = null;
    assertTrue(isEmpty(string));

    Map map = null;
    assertTrue(isEmpty(map));

    ImplementsCollection<String> collection = null;
    assertTrue(isEmpty(collection));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testEmptyIsEmpty() {
    Custom custom = Custom.builder().empty(true).build();
    assertTrue(isEmpty(custom));

    List<String> list = new ArrayList();
    assertTrue(isEmpty(list));

    String string = "";
    assertTrue(isEmpty(string));

    Map<String, String> map = new HashMap();
    assertTrue(isEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertTrue(isEmpty(collection));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNotEmptyIsEmptyFalse() {
    Custom custom = Custom.builder().empty(false).build();
    assertFalse(isEmpty(custom));

    List<String> list = asList("foo");
    assertFalse(isEmpty(list));

    String string = "some";
    assertFalse(isEmpty(string));

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertFalse(isEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertFalse(isEmpty(collection));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNullIsNotEmptyFalse() {
    Custom custom = null;
    assertFalse(isNotEmpty(custom));

    List list = null;
    assertFalse(isNotEmpty(list));

    String string = null;
    assertFalse(isNotEmpty(string));

    Map map = null;
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = null;
    assertFalse(isNotEmpty(collection));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testEmptyIsNotEmptyFalse() {
    Custom custom = Custom.builder().empty(true).build();
    assertFalse(isNotEmpty(custom));

    List<String> list = new ArrayList();
    assertFalse(isNotEmpty(list));

    String string = "";
    assertFalse(isNotEmpty(string));

    Map<String, String> map = new HashMap();
    assertFalse(isNotEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(true);
    assertFalse(isNotEmpty(collection));
  }

  @Test
  @Category({FastUnitTests.class})
  public void testNotEmptyIsNotEmpty() {
    Custom custom = Custom.builder().empty(false).build();
    assertTrue(isNotEmpty(custom));

    List<String> list = asList("foo");
    assertTrue(isNotEmpty(list));

    String string = "some";
    assertTrue(isNotEmpty(string));

    Map<String, String> map = new HashMap() {
      { put("foo", "foo"); }
    };
    assertTrue(isNotEmpty(map));

    ImplementsCollection<String> collection = new ImplementsCollection(false);
    assertTrue(isNotEmpty(collection));
  }
}
