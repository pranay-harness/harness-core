package io.harness.remote;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;
import io.harness.serializer.jackson.HarnessJacksonModule;
import lombok.experimental.UtilityClass;
import software.wings.jersey.JsonViews;

import java.util.List;

@UtilityClass
public class NGObjectMapperHelper {
  public void configureNGObjectMapper(final ObjectMapper mapper) {
    final AnnotationAwareJsonSubtypeResolver subtypeResolver =
        AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver());
    mapper.setSubtypeResolver(subtypeResolver);
    mapper.setConfig(mapper.getSerializationConfig().withView(JsonViews.Public.class));
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
      @Override
      public List<NamedType> findSubtypes(Annotated a) {
        final List<NamedType> subtypesFromSuper = super.findSubtypes(a);
        if (isNotEmpty(subtypesFromSuper)) {
          return subtypesFromSuper;
        }
        return emptyIfNull(subtypeResolver.findSubtypes(a));
      }
    });
    mapper.registerModule(new ProtobufModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new JavaTimeModule());
    mapper.registerModule(new HarnessJacksonModule());
  }
}
