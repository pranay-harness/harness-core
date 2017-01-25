package software.wings.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Created by peeyushaggarwal on 1/23/17.
 */
public class KryoUtils {
  private static final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(() -> new Kryo());
  private static final Logger logger = LoggerFactory.getLogger(KryoUtils.class);

  public static String asString(Object obj) {
    return Base64.encodeBase64String(asBytes(obj));
  }

  public static byte[] asBytes(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Output output = new Output(baos);
      kryos.get().writeClassAndObject(output, obj);
      output.flush();
      return baos.toByteArray();
    } catch (Exception exception) {
      logger.error(exception.getMessage(), exception);
      throw new RuntimeException(exception);
    }
  }

  public static Object asObject(byte[] bytes) {
    Input input = new Input(bytes);
    Object obj = kryos.get().readClassAndObject(input);
    input.close();
    return obj;
  }

  public static Object asObject(String base64) {
    return asObject(Base64.decodeBase64(base64));
  }
}
