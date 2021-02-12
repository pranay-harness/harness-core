package io.harness.callgraph.util;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.harness.callgraph.MockitoRule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.security.ProtectionDomain;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class TestClassesMatcherTest {
  @Rule public TestRule mockitoRule = new MockitoRule(this);

  @Mock private TypeDescription typeDescription;

  @Mock private ClassLoader classLoader;

  @Mock private JavaModule module;

  @Mock private ProtectionDomain protectionDomain;

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testMatches() throws Exception {
    TestClassesMatcher rawMatcher = new TestClassesMatcher(false);
    assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(true));
  }

  private static class Foo { /* empty */
  }
}
