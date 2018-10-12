package software.wings.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static software.wings.utils.Util.escapifyString;
import static software.wings.utils.Util.getNameWithNextRevision;

import com.google.common.collect.ImmutableList;

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
    assertThat(map.size()).isEqualTo(3);
  }

  @Test
  public void testEscapifyString() {
    assertThat(escapifyString("ab\\")).isEqualTo("ab\\\\");
    assertThat(escapifyString("ab\\cd")).isEqualTo("ab\\cd");
    assertThat(escapifyString("a\"b")).isEqualTo("a\\\"b");
    assertThat(escapifyString("a'b")).isEqualTo("a'b");
    assertThat(escapifyString("a`b")).isEqualTo("a\\`b");
    assertThat(escapifyString("a(b")).isEqualTo("a(b");
    assertThat(escapifyString("a)b")).isEqualTo("a)b");
    assertThat(escapifyString("a|b")).isEqualTo("a|b");
    assertThat(escapifyString("a<b")).isEqualTo("a<b");
    assertThat(escapifyString("a>b")).isEqualTo("a>b");
    assertThat(escapifyString("a;b")).isEqualTo("a;b");
    assertThat(escapifyString("a b")).isEqualTo("a b");
  }

  @Test
  public void testGetNameWithNextRevision() {
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def"), "abc-def")).isEqualTo("abc-def-1");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def-1", "abc-def"), "abc-def")).isEqualTo("abc-def-2");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def-"), "abc-def-")).isEqualTo("abc-def--1");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def-", "abc-def--1"), "abc-def-")).isEqualTo("abc-def--2");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def-1"), "abc-def-1")).isEqualTo("abc-def-1-1");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def-1", "abc-def-1-1"), "abc-def-1"))
        .isEqualTo("abc-def-1-2");
    assertThat(getNameWithNextRevision(ImmutableList.of("abc-def", "abc-def-2", "abc-def-3", "abc-def-1"), "abc-def"))
        .isEqualTo("abc-def-4");
    assertThat(getNameWithNextRevision(
                   ImmutableList.of("abc-def", "abc-def-2", "abc-def-3", "abc-def-1", "abc-def-5", "abc-def-6",
                       "abc-def-4", "abc-def-8", "abc-def-7", "abc-def-9", "abc-def-10", "abc-def-12", "abc-def-11"),
                   "abc-def"))
        .isEqualTo("abc-def-13");
  }
}
