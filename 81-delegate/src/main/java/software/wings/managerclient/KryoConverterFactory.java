package software.wings.managerclient;

import static java.util.Arrays.stream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Converter.Factory;
import retrofit2.Retrofit;
import software.wings.utils.KryoUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by peeyushaggarwal on 1/13/17.
 */
public class KryoConverterFactory extends Factory {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-kryo");

  @Override
  public Converter<?, RequestBody> requestBodyConverter(
      Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    if (stream(methodAnnotations)
            .anyMatch(annotation -> annotation.annotationType().isAssignableFrom(KryoRequest.class))) {
      return value -> RequestBody.create(MEDIA_TYPE, KryoUtils.asBytes(value));
    }
    return null;
  }

  @Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
    if (stream(annotations).anyMatch(annotation -> annotation.annotationType().isAssignableFrom(KryoResponse.class))) {
      return value -> {
        try {
          return KryoUtils.asObject(value.bytes());
        } finally {
          value.close();
        }
      };
    }
    return null;
  }
}
