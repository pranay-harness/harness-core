package software.wings.utils;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.BRETT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.Utils.escapifyString;
import static software.wings.utils.Utils.getNameWithNextRevision;

import com.google.common.collect.ImmutableList;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.NameValuePair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testToProperties() {
    List<NameValuePair> nameValuePairList = new ArrayList<>();
    nameValuePairList.add(NameValuePair.builder().name("n1").value("v1").build());
    nameValuePairList.add(NameValuePair.builder().name("n2").value("v2").build());
    nameValuePairList.add(NameValuePair.builder().name("n3").value(null).build());

    Map map = Utils.toProperties(nameValuePairList);
    assertThat(map).isNotNull();
    assertThat(map.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
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
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
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

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void testAppendPathToBaseUrl() {
    assertThat(Utils.appendPathToBaseUrl("https://example.com", "hello")).isEqualTo("https://example.com/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/", "hello")).isEqualTo("https://example.com/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/", "/hello")).isEqualTo("https://example.com/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com:8080", "hello"))
        .isEqualTo("https://example.com:8080/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com:8080/", "hello"))
        .isEqualTo("https://example.com:8080/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com:8080/", "/hello"))
        .isEqualTo("https://example.com:8080/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/path1/path2", "hello"))
        .isEqualTo("https://example.com/path1/path2/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/path1/path2", "/hello"))
        .isEqualTo("https://example.com/path1/path2/hello");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/path1/path2/", "/hello/"))
        .isEqualTo("https://example.com/path1/path2/hello/");
    assertThat(Utils.appendPathToBaseUrl("https://example.com/path1/path2/", "/hello/q=abc&abc=%20abc"))
        .isEqualTo("https://example.com/path1/path2/hello/q=abc&abc=%20abc");
    assertThat(Utils.appendPathToBaseUrl("https://example.com", "q=abc&abc=%20abc"))
        .isEqualTo("https://example.com/q=abc&abc=%20abc");
    assertThat(Utils.appendPathToBaseUrl("https://example.com", "./hello")).isEqualTo("https://example.com/hello");
    assertThat(Utils.appendPathToBaseUrl("http://example.com", "./hello")).isEqualTo("http://example.com/hello");
  }
}
