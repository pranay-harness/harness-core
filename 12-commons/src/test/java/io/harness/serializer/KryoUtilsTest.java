package io.harness.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.IntMap;
import io.harness.threading.Concurrent;
import org.junit.Test;

public class KryoUtilsTest {
  @Test
  public void shouldInitConcurrently() {
    Concurrent.test(3, i -> { KryoUtils.clone(1); });
    Concurrent.test(10, i -> { KryoUtils.clone(1); });
    Concurrent.test(10, i -> { KryoUtils.clone(1); });
  }

  @Test
  public void shouldGetXpath() {
    String test = "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit...";
    final byte[] deflatedBytes = KryoUtils.asDeflatedBytes(test);
    String inflatedObject = (String) KryoUtils.asInflatedObject(deflatedBytes);

    assertThat(test).isEqualTo(inflatedObject);
  }

  @Test
  public void testRegistrarIssues() {
    final ClassResolver classResolver = new ClassResolver();
    Kryo kryo = new HKryo(classResolver);

    kryo.register(KryoUtilsTest.class, 123456);

    assertThatThrownBy(() -> kryo.register(KryoUtilsTest.class, 123457))
        .hasMessage("The class io.harness.serializer.KryoUtilsTest was already registered with id 123456,"
            + " do not double register it with 123457");

    final IntMap<Registration> previousState = new IntMap<>(classResolver.getRegistrations());
    kryo.register(ClassResolver.class, 123456);

    assertThatThrownBy(() -> KryoUtils.check(previousState, classResolver.getRegistrations()))
        .hasMessage("The id 123456 changed its class from io.harness.serializer.KryoUtilsTest"
            + " to io.harness.serializer.ClassResolver");
  }
}
