package io.harness.yaml.core.deserializer;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
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

    Class<?> subClass = null;

    // Determine type name from yaml property obtained by getTypePropertyName method
    if (root.has(getTypePropertyName())) {
      JsonNode typeNode = root.findValue(getTypePropertyName());
      String type = typeNode.asText();
      if (typeNode.isNull() || isEmpty(type)) {
        throw ctxt.mappingException("Type property: '" + getTypePropertyName() + "' is empty");
      }

      Collection<NamedType> subtypeClasses = DeserializerHelper.getSubtypeClasses(codec, getType());
      Optional<NamedType> namedType = subtypeClasses.stream().filter(nt -> type.equals(nt.getName())).findFirst();

      if (namedType.isPresent()) {
        subClass = namedType.get().getType();
      }
      if (subClass == null) {
        throw ctxt.mappingException("No class definition found for type: '" + type + "'");
      }
    } else {
      throw ctxt.mappingException("Cannot find type property: '" + getTypePropertyName() + "'");
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
}
