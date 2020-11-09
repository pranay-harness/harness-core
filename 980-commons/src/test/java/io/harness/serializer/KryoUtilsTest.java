package io.harness.serializer;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.esotericsoftware.kryo.Kryo;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.threading.Concurrent;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KryoUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldInitConcurrently() {
    Concurrent.test(3, i -> { KryoUtils.clone(1); });
    Concurrent.test(10, i -> { KryoUtils.clone(1); });
    Concurrent.test(10, i -> { KryoUtils.clone(1); });
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetXpath() {
    String test = "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...";
    final byte[] deflatedBytes = KryoUtils.asDeflatedBytes(test);
    String inflatedObject = (String) KryoUtils.asInflatedObject(deflatedBytes);

    assertThat(test).isEqualTo(inflatedObject);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testRegistrarIssues() {
    final ClassResolver classResolver = new ClassResolver();
    Kryo kryo = new HKryo(classResolver);

    kryo.register(KryoUtilsTest.class, 123456);

    assertThatThrownBy(() -> kryo.register(KryoUtilsTest.class, 123457))
        .hasMessage("The class io.harness.serializer.KryoUtilsTest was already registered with id 123456,"
            + " do not double register it with 123457");

    assertThatThrownBy(() -> kryo.register(ClassResolver.class, 123456))
        .hasMessage("The id is already used by class io.harness.serializer.KryoUtilsTest");
  }
}
