package software.wings.managerclient;

import static java.util.Arrays.stream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Converter.Factory;
import retrofit2.Retrofit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
      return new KyroRequestConverter();
    } else {
      return null;
    }
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    if (stream(annotations)
            .filter(annotation -> annotation.annotationType().isAssignableFrom(KryoResponse.class))
            .findFirst()
            .isPresent()) {
      return new KryoResponseConverter(
          ((sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl) type).getRawType());
    } else {
      return null;
    }
  }

  private static class KryoResponseConverter<T> implements Converter<ResponseBody, T> {
    private Class<T> klass;

    public KryoResponseConverter(Class<T> type) {
      klass = type;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
      Input input = new Input(value.bytes());
      T someObject = (T) kryos.get().readClassAndObject(input);
      input.close();
      return someObject;
    }
  }

  private class KyroRequestConverter<T> implements Converter<T, RequestBody> {
    @Override
    public RequestBody convert(T value) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Output output = new Output(baos);
      kryos.get().writeClassAndObject(output, value);
      output.flush();
      return RequestBody.create(MEDIA_TYPE, baos.toByteArray());
    }
  }
}
