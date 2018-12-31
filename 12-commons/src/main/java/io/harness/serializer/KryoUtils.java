package io.harness.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.binary.Base64;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@SuppressFBWarnings("DM_EXIT")
public class KryoUtils {
  private static final Logger logger = LoggerFactory.getLogger(KryoUtils.class);
  private static KryoPool pool =
      new KryoPool
          .Builder(() -> {
            Kryo kryo = new Kryo();
            // Log.TRACE();
            kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
            kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
                FieldSerializer.CachedFieldNameStrategy.EXTENDED);
            kryo.getFieldSerializerConfig().setCopyTransient(false);

            kryo.setRegistrationRequired(true); // Don't change

            try {
              Reflections reflections = new Reflections("io.harness.serializer.kryo");
              for (Class clazz : reflections.getSubTypesOf(KryoRegistrar.class)) {
                Constructor<?> constructor = clazz.getConstructor();
                final KryoRegistrar kryoRegistrar = (KryoRegistrar) constructor.newInstance();
                kryoRegistrar.register(kryo);
              }

            } catch (Exception e) {
              logger.error("Failed to initialize kryo", e);
              System.exit(1);
            }

            return kryo;
          })
          .softReferences()
          .build();

  public static String asString(Object obj) {
    return Base64.encodeBase64String(asBytes(obj));
  }

  public static byte[] asBytes(Object obj) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    toStream(obj, outputStream);
    return outputStream.toByteArray();
  }

  public static byte[] asDeflatedBytes(Object obj) {
    try {
      final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      final DeflaterOutputStream outputStream = new DeflaterOutputStream(byteStream);
      toStream(obj, outputStream);
      outputStream.finish();
      return byteStream.toByteArray();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private static void toStream(Object obj, OutputStream outputStream) {
    try {
      Output output = new Output(outputStream);
      pool.run(kryo -> {
        kryo.writeClassAndObject(output, obj);
        return null;
      });
      output.flush();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static void writeBytes(Object obj, OutputStream outputStream) {
    try {
      Output output = new Output(outputStream);
      pool.run(kryo -> {
        kryo.writeClassAndObject(output, obj);
        return null;
      });
      output.flush();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  public static <T> T clone(T object) {
    return pool.run(kryo -> kryo.copy(object));
  }

  public static Object asObject(byte[] bytes) {
    Input input = new Input(bytes);
    Object obj = pool.run(kryo -> kryo.readClassAndObject(input));
    input.close();
    return obj;
  }

  public static Object asInflatedObject(byte[] bytes) {
    Input input = new Input(new InflaterInputStream(new Input(bytes)));
    Object obj = pool.run(kryo -> kryo.readClassAndObject(input));
    input.close();
    return obj;
  }

  public static Object asObject(String base64) {
    return asObject(Base64.decodeBase64(base64));
  }
}
