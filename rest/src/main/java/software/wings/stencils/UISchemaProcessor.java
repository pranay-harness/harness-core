package software.wings.stencils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.utils.JsonUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by peeyushaggarwal on 4/6/17.
 */
public class UISchemaProcessor {
  private static final Logger logger = LoggerFactory.getLogger(UISchemaProcessor.class);

  public static <T> Map<String, Object> generate(Class<T> stencilClass) throws IntrospectionException {
    List<String> order = processFieldOrder(stencilClass);
    return ImmutableMap.of("ui:order", order);
  }

  private static <T> List<String> processFieldOrder(Class<T> stencilClass) throws IntrospectionException {
    return Stream
        .concat(fieldStream(stencilClass)
                    .filter(field -> field.getAnnotation(UIOrder.class) != null)
                    .map(field -> Pair.of(field.getName(), field.getAnnotation(UIOrder.class).value())),
            stream(Introspector.getBeanInfo(stencilClass).getPropertyDescriptors())
                .filter(propertyDescriptor -> propertyDescriptor.getReadMethod().getAnnotation(UIOrder.class) != null)
                .map(propertyDescriptor
                    -> Pair.of(propertyDescriptor.getName(),
                        propertyDescriptor.getReadMethod().getAnnotation(UIOrder.class).value())))
        .sorted(Comparator.comparing(Pair::getRight))
        .map(Pair::getLeft)
        .distinct()
        .collect(toList());
  }

  private static Stream<Field> fieldStream(Class<?> klass) {
    if (klass != null && klass != Object.class) {
      return Stream.concat(stream(klass.getDeclaredFields()), fieldStream(klass.getSuperclass()));
    } else {
      return Stream.empty();
    }
  }

  public static void main(String... args) throws IntrospectionException {
    logger.info(JsonUtils.asJson(UISchemaProcessor.generate(JenkinsArtifactStream.class)));
  }
}
