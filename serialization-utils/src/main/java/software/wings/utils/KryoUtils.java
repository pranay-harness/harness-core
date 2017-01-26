package software.wings.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class KryoUtils {
  private static final KryoPool pool = new KryoPool
                                           .Builder(() -> {
                                             Kryo kryo = new Kryo();
                                             kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
                                                 FieldSerializer.CachedFieldNameStrategy.EXTENDED);
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
      logger.error(exception.getMessage(), exception);
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
      logger.error(exception.getMessage(), exception);
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
}
