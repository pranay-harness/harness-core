package io.harness.yaml.core.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import lombok.experimental.UtilityClass;

import java.util.Collection;

@UtilityClass
public class DeserializerHelper {
  public Collection<NamedType> getSubtypeClasses(ObjectMapper mapper, Class<?> clazz) {
    MapperConfig<?> config = mapper.getDeserializationConfig();
    AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(clazz, config);
    return mapper.getSubtypeResolver().collectAndResolveSubtypesByClass(config, ac);
  }
}
