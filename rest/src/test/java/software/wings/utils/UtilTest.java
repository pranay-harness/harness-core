package software.wings.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static software.wings.utils.Util.escapifyString;
import static software.wings.utils.Util.getNameWithNextRevision;

import org.junit.Test;
import software.wings.beans.NameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UtilTest {
  @Test
  public void testToProperties() {
    List<NameValuePair> nameValuePairList = new ArrayList<>();
    nameValuePairList.add(NameValuePair.builder().name("n1").value("v1").build());
    nameValuePairList.add(NameValuePair.builder().name("n2").value("v2").build());
    nameValuePairList.add(NameValuePair.builder().name("n3").value(null).build());

    Map map = Util.toProperties(nameValuePairList);
    assertNotNull(map);
    assertEquals(3, map.size());
  }

  @Test
  public void testEscapifyString() {
    assertEquals(escapifyString("ab\\"), "ab\\\\");
    assertEquals(escapifyString("ab\\cd"), "ab\\cd");
    assertEquals(escapifyString("a\"b"), "a\\\"b");
    assertEquals(escapifyString("a'b"), "a'b");
    assertEquals(escapifyString("a`b"), "a\\`b");
    assertEquals(escapifyString("a(b"), "a(b");
    assertEquals(escapifyString("a)b"), "a)b");
    assertEquals(escapifyString("a|b"), "a|b");
    assertEquals(escapifyString("a<b"), "a<b");
    assertEquals(escapifyString("a>b"), "a>b");
    assertEquals(escapifyString("a;b"), "a;b");
    assertEquals(escapifyString("a b"), "a b");
  }

  @Test
  public void testGetNameWithNextRevision() {
    assertEquals(getNameWithNextRevision("todolist_war_copy2-do-not-delete", "todolist_war_copy2-do-not-delete"),
        "todolist_war_copy2-do-not-delete-1");
    assertEquals(getNameWithNextRevision("todolist_war_copy2-do-not-delete-1", "todolist_war_copy2-do-not-delete"),
        "todolist_war_copy2-do-not-delete-2");
  }
}
