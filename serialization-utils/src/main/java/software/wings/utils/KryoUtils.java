package software.wings.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;
import de.javakaffee.kryoserializers.cglib.CGLibProxySerializer;
import de.javakaffee.kryoserializers.guava.ArrayListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.HashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableListSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ImmutableSetSerializer;
import de.javakaffee.kryoserializers.guava.LinkedHashMultimapSerializer;
import de.javakaffee.kryoserializers.guava.LinkedListMultimapSerializer;
import de.javakaffee.kryoserializers.guava.ReverseListSerializer;
import de.javakaffee.kryoserializers.guava.TreeMultimapSerializer;
import de.javakaffee.kryoserializers.guava.UnmodifiableNavigableSetSerializer;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.GregorianCalendar;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class KryoUtils {
  private static final KryoPool pool =
      new KryoPool
          .Builder(() -> {
            Kryo kryo = new Kryo();
            // Log.TRACE();
            kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
                FieldSerializer.CachedFieldNameStrategy.EXTENDED);
            kryo.getFieldSerializerConfig().setCopyTransient(false);
            kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
            kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
            kryo.register(InvocationHandler.class, new JdkProxySerializer());
            UnmodifiableCollectionsSerializer.registerSerializers(kryo);
            SynchronizedCollectionsSerializer.registerSerializers(kryo);

            // custom serializers for non-jdk libs

            // register CGLibProxySerializer, works in combination with the appropriate action in
            // handleUnregisteredClass (see below)
            kryo.register(CGLibProxySerializer.CGLibProxyMarker.class, new CGLibProxySerializer());
            // guava ImmutableList, ImmutableSet, ImmutableMap, ImmutableMultimap, ReverseList, UnmodifiableNavigableSet
            ImmutableListSerializer.registerSerializers(kryo);
            ImmutableSetSerializer.registerSerializers(kryo);
            ImmutableMapSerializer.registerSerializers(kryo);
            ImmutableMultimapSerializer.registerSerializers(kryo);
            ReverseListSerializer.registerSerializers(kryo);
            UnmodifiableNavigableSetSerializer.registerSerializers(kryo);
            // guava ArrayListMultimap, HashMultimap, LinkedHashMultimap, LinkedListMultimap, TreeMultimap
            ArrayListMultimapSerializer.registerSerializers(kryo);
            HashMultimapSerializer.registerSerializers(kryo);
            LinkedHashMultimapSerializer.registerSerializers(kryo);
            LinkedListMultimapSerializer.registerSerializers(kryo);
            TreeMultimapSerializer.registerSerializers(kryo);
            return kryo;
          })
          .softReferences()
          .build();
  private static final Logger logger = LoggerFactory.getLogger(KryoUtils.class);

  public static String asString(Object obj) {
    return Base64.encodeBase64String(asBytes(obj));
  }

  public static byte[] asBytes(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Output output = new Output(baos);
      pool.run(kryo -> {
        kryo.writeClassAndObject(output, obj);
        return null;
      });
      output.flush();
      return baos.toByteArray();
    } catch (Exception exception) {
      logException(exception.getMessage(), exception);
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
      logException(exception.getMessage(), exception);
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

  public static Object asObject(String base64) {
    return asObject(Base64.decodeBase64(base64));
  }

  private static void logException(String msg, Throwable t) {
    logger.error(msg, t);
    while (t != null) {
      logger.error("***** Caused by: " + t.getClass().getCanonicalName()
          + (t.getMessage() != null ? ": " + t.getMessage() : ""));
      Arrays.stream(t.getStackTrace()).forEach(elem -> logger.error(" --- Trace: " + elem));
      t = t.getCause();
    }
  }
}
