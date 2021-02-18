package io.harness.callgraph.util;

import static io.harness.rule.OwnerRule.SHIVAKUMAR;

import io.harness.callgraph.MockitoRule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.security.ProtectionDomain;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class SourceClassesMatcherTest {
  @Rule public TestRule mockitoRule = new MockitoRule(this);

  @Mock private TypeDescription typeDescription;

  @Mock private ClassLoader classLoader;

  @Mock private JavaModule module;

  @Mock private ProtectionDomain protectionDomain;

  @Test
  @Owner(developers = SHIVAKUMAR)
  @Category(UnitTests.class)
  public void testMatches() throws Exception {
    SourceClassesMatcher rawMatcher = new SourceClassesMatcher(false);
    Assertions.assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).isTrue();
  }

  private static class Foo { /* empty */
  }
}
