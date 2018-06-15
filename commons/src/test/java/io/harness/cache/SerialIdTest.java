package io.harness.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import org.junit.Test;

import java.io.ObjectStreamClass;

class Dummy implements Distributable {
  public static final long structureHash = ObjectStreamClass.lookup(Dummy.class).getSerialVersionUID();

  @Override
  public long structureHash() {
    return structureHash;
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
  public void testSanity() {
    final Dummy dummy = new Dummy();
    assertThat(dummy.structureHash()).isEqualTo(-7694367900928271898L);
  }
}
