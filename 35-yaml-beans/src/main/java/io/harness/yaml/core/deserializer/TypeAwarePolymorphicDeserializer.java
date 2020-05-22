package io.harness.yaml.core.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * Polymorphic deserializer is used in cases where we have multiple levels of parallelism.
 * This deserializer will extract type from existing property and create default deserializer that
 * will handle subtype conversion.
 * Jackson library can only resolve first level of parallelism if we have mixed type ids.
 * JsonTypeInfo provides few methods for id notation. In case multiple levels of polymorphism each described using
 * different we need to resolve second level with custom deserializer.
 * @param <T> base type for conversion
 */
public abstract class TypeAwarePolymorphicDeserializer<T> extends StdDeserializer<T> implements TypeDescriptor {
  public TypeAwarePolymorphicDeserializer() {
    this(null);
  }
  public TypeAwarePolymorphicDeserializer(Class vc) {
    super(vc);
  }

  @Override
  public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    ObjectMapper codec = (ObjectMapper) jp.getCodec();
    ObjectNode root = codec.readTree(jp);

    Class<?> subClass;

    // Determine type name from yaml property obtained by getTypePropertyName method
    if (root.has(getTypePropertyName())) {
      String type = root.findValue(getTypePropertyName()).asText();

      Collection<NamedType> subtypeClasses = getSubtypeClasses(codec, getType());
      Optional<NamedType> namedType = subtypeClasses.stream().filter(nt -> type.equals(nt.getName())).findFirst();

      if (namedType.isPresent()) {
        subClass = namedType.get().getType();
      } else {
        return null;
      }
    } else {
      return null;
    }

    // Create default deserializer for sub type
    DeserializationConfig config = ctxt.getConfig();
    JavaType javaType = TypeFactory.defaultInstance().constructType(subClass);
    JsonDeserializer<Object> defaultDeserializer =
        BeanDeserializerFactory.instance.buildBeanDeserializer(ctxt, javaType, config.introspect(javaType));

    if (defaultDeserializer instanceof ResolvableDeserializer) {
      ((ResolvableDeserializer) defaultDeserializer).resolve(ctxt);
    }

    // Parse object using subtype default deserializer
    JsonParser treeParser = codec.treeAsTokens(root);
    config.initialize(treeParser);

    if (treeParser.getCurrentToken() == null) {
      treeParser.nextToken();
    }

    return (T) defaultDeserializer.deserialize(treeParser, ctxt);
  }

  private Collection<NamedType> getSubtypeClasses(ObjectMapper mapper, Class<?> clazz) {
    MapperConfig<?> config = mapper.getDeserializationConfig();
    AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(clazz, config);
    return mapper.getSubtypeResolver().collectAndResolveSubtypesByClass(config, ac);
  }
}
