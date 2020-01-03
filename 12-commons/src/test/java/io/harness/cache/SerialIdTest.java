package io.harness.cache;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ObjectStreamClass;
import java.util.List;

class Dummy implements Distributable, Nominal {
  public static final long STRUCTURE_HASH = ObjectStreamClass.lookup(Dummy.class).getSerialVersionUID();

  @Override
  public long structureHash() {
    return STRUCTURE_HASH;
  }

  @Override
  public long algorithmId() {
    return 0;
  }

  @Override
  public long contextHash() {
    return 0;
  }

  @Override
  public String key() {
    return null;
  }

  @Override
  public List<String> parameters() {
    return null;
  }
}

public class SerialIdTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testSanity() {
    final Dummy dummy = new Dummy();
    assertThat(dummy.structureHash()).isEqualTo(37626829568819767L);
  }
}
