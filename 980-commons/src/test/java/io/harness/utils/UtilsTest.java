package io.harness.utils;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UtilsTest {
  interface A {}

  class B implements A {}

  class C implements A {}

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetFirstInstance() {
    B b1 = new B();
    B b2 = new B();
    C c = new C();
    List<A> list = Arrays.asList(b1, b2, c);
    B outB = Utils.getFirstInstance(list, B.class);
    C outC = Utils.getFirstInstance(list, C.class);

    assertThat(outB).isEqualTo(b1);
    assertThat(outB).isNotEqualTo(b2);
    assertThat(outC).isEqualTo(c);
    assertThat(outC).isNotEqualTo(b1);

    list = Collections.singletonList(b1);
    outC = Utils.getFirstInstance(list, C.class);
    assertThat(outC).isEqualTo(null);
  }
}