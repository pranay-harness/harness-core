package io.harness.serializer;

import static java.lang.String.format;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.util.IntMap;
import io.harness.exception.GeneralException;
import io.harness.reflection.CodeUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.reflections.Reflections;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@UtilityClass
@Slf4j
@Deprecated // There is a race between the kryo initialization through reflection and the loading of all jars.
            // In some rare cases this leaves the kryo registration map incomplete and it makes a lot of the
            // communications to fail. Use KryoSerializer instead.
public class KryoUtils {
  public static synchronized Kryo kryo() {
    final ClassResolver classResolver = new ClassResolver();
    HKryo kryo = new HKryo(classResolver);

    try {
      Reflections reflections = new Reflections("io.harness.serializer.kryo");
      for (Class clazz : reflections.getSubTypesOf(KryoRegistrar.class)) {
        Constructor<?> constructor = clazz.getConstructor();
        final KryoRegistrar kryoRegistrar = (KryoRegistrar) constructor.newInstance();

        final IntMap<Registration> previousState = new IntMap<>(classResolver.getRegistrations());
        kryo.setCurrentLocation(CodeUtils.location(kryoRegistrar.getClass()));
        kryoRegistrar.register(kryo);

        try {
          KryoSerializer.check(previousState, classResolver.getRegistrations());
        } catch (Exception exception) {
          throw new IllegalStateException(
              format("Check for registration of %s failed", clazz.getCanonicalName()), exception);
        }
      }

    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new GeneralException("Failed initializing kryo", e);
    }

    return kryo;
  }

  private static final KryoPool pool = new KryoPool.Builder(KryoUtils::kryo).softReferences().build();

  public static String asString(Object obj) {
    return Base64.encodeBase64String(asBytes(obj));
  }

  public static byte[] asBytes(Object obj) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    writeToStream(obj, outputStream);
    return outputStream.toByteArray();
  }

  public static byte[] asDeflatedBytes(Object obj) {
    try {
      final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      final DeflaterOutputStream outputStream = new DeflaterOutputStream(byteStream);
      writeToStream(obj, outputStream);
      outputStream.finish();
      return byteStream.toByteArray();
    } catch (Exception exception) {
      throw new RuntimeException(exception);
    }
  }

  private static void writeToStream(Object obj, OutputStream outputStream) {
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

  public static boolean isRegistered(Class cls) {
    return pool.run(kryo -> kryo.getClassResolver().getRegistration(cls) != null);
  }
}
