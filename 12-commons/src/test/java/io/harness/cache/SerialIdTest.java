package io.harness.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ObjectStreamClass;

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
}

public class SerialIdTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testSanity() {
    final Dummy dummy = new Dummy();
    assertThat(dummy.structureHash()).isEqualTo(492977267398168896L);
  }
}
