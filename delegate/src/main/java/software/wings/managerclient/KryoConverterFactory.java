package software.wings.managerclient;

import static java.util.Arrays.stream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import retrofit2.Converter;
import retrofit2.Converter.Factory;
import retrofit2.Retrofit;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by peeyushaggarwal on 1/13/17.
 */
public class KryoConverterFactory extends Factory {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-kryo");

  private static final ThreadLocal<Kryo> kryos = ThreadLocal.withInitial(() -> new Kryo());

  @Override
  public Converter<?, RequestBody> requestBodyConverter(
      Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (stream(methodAnnotations)
            .filter(annotation -> annotation.annotationType().isAssignableFrom(KryoRequest.class))
            .findFirst()
            .isPresent()) {
      return value -> {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryos.get().writeClassAndObject(output, value);
        output.flush();
        return RequestBody.create(MEDIA_TYPE, baos.toByteArray());
      };
    }
    return null;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    if (stream(annotations)
            .filter(annotation -> annotation.annotationType().isAssignableFrom(KryoResponse.class))
            .findFirst()
            .isPresent()) {
      return value -> {
        try {
          Input input = new Input(value.bytes());
          Object someObject = kryos.get().readClassAndObject(input);
          input.close();
          return someObject;
        } finally {
          IOUtils.closeQuietly(value);
        }
      };
    }
    return null;
  }
}
