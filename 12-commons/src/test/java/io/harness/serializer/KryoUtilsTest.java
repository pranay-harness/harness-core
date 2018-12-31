package io.harness.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.util.IntMap;
import org.junit.Test;

public class KryoUtilsTest {
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

    kryo.register(int[].class, 10);

    assertThatThrownBy(() -> kryo.register(int[].class, 11))
        .hasMessage("The class int[] was already registered with id 10, do not double register it with 11");

    final IntMap<Registration> previousState = new IntMap<>(classResolver.getRegistrations());
    kryo.register(short[].class, 10);

    assertThatThrownBy(() -> KryoUtils.check(previousState, classResolver.getRegistrations()))
        .hasMessage("The id 10 changed its class from int[] to short[]");
  }
}
