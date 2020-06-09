package io.harness.yaml.core.jsontype;

import com.google.common.collect.Sets;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import io.harness.serializer.JsonSubtypeResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Slf4j
public class AnnotationAwareJsonSubtypeResolver extends JsonSubtypeResolver {
  private AnnotationAwareJsonSubtypeResolver(SubtypeResolver subtypeResolver) {
    super(subtypeResolver);
  }

  public static AnnotationAwareJsonSubtypeResolver newInstance(SubtypeResolver subtypeResolver) {
    return new AnnotationAwareJsonSubtypeResolver(subtypeResolver);
  }

  @Override
  public Collection<NamedType> collectAndResolveSubtypes(
      AnnotatedMember property, MapperConfig<?> config, AnnotationIntrospector ai, JavaType baseType) {
    final Collection<NamedType> returnValue = subtypeResolver.collectAndResolveSubtypes(property, config, ai, baseType);
    final Set<NamedType> newReturnValue = Sets.newLinkedHashSet(returnValue);
    if (newReturnValue.size() == 1) {
      try {
        newReturnValue.addAll(classListLoadingCache.get(getRawType(property)));
      } catch (ExecutionException e) {
        logger.error("Error while getting subtypes", e);
      }
    }
    return newReturnValue;
  }

  private Class<?> getRawType(AnnotatedMember member) {
    if (member instanceof AnnotatedMethod) {
      final AnnotatedMethod method = (AnnotatedMethod) member;
      if (method.getParameterCount() > 0) {
        return method.getRawParameterType(0);
      }
    }
    return member.getRawType();
  }
}
