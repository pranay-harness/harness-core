package software.wings.utils;

import static java.util.stream.Collectors.toList;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.SubtypeResolver;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 9/30/16.
 */
public class JsonSubtypeResolver extends SubtypeResolver {
  private static final Logger logger = LoggerFactory.getLogger(JsonSubtypeResolver.class);
  private SubtypeResolver subtypeResolver;

  private LoadingCache<Class<?>, List<NamedType>> classListLoadingCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(10, TimeUnit.MINUTES)
          .build(new CacheLoader<Class<?>, List<NamedType>>() {
            public List<NamedType> load(Class<?> key) {
              Reflections reflections = new Reflections("software.wings");
              return reflections.getSubTypesOf(key)
                  .stream()
                  .filter(subClass -> subClass.isAnnotationPresent(JsonTypeName.class))
                  .map(subClass -> new NamedType(subClass, subClass.getAnnotation(JsonTypeName.class).value()))
                  .collect(toList());
            }
          });

  /**
   * Instantiates a new Json subtype resolver.
   *
   * @param subtypeResolver the subtype resolver
   */
  public JsonSubtypeResolver(SubtypeResolver subtypeResolver) {
    this.subtypeResolver = subtypeResolver;
  }

  @Override
  public void registerSubtypes(NamedType... types) {
    subtypeResolver.registerSubtypes(types);
  }

  @Override
  public void registerSubtypes(Class<?>... classes) {
    subtypeResolver.registerSubtypes(classes);
  }

  @Override
  public Collection<NamedType> collectAndResolveSubtypes(
      AnnotatedMember property, MapperConfig<?> config, AnnotationIntrospector ai, JavaType baseType) {
    Collection<NamedType> returnValue = subtypeResolver.collectAndResolveSubtypes(property, config, ai, baseType);
    LinkedHashSet<NamedType> newReturnValue = Sets.newLinkedHashSet(returnValue);
    if (newReturnValue.size() == 1) {
      try {
        newReturnValue.addAll(classListLoadingCache.get(property.getRawType()));
      } catch (ExecutionException e) {
        logger.error("", e);
      }
    }
    return returnValue;
  }

  @Override
  public Collection<NamedType> collectAndResolveSubtypes(
      AnnotatedClass baseType, MapperConfig<?> config, AnnotationIntrospector ai) {
    Collection<NamedType> returnValue = subtypeResolver.collectAndResolveSubtypes(baseType, config, ai);
    LinkedHashSet<NamedType> newReturnValue = Sets.newLinkedHashSet(returnValue);
    if (newReturnValue.size() == 1) {
      try {
        newReturnValue.addAll(classListLoadingCache.get(baseType.getAnnotated()));
      } catch (ExecutionException e) {
        logger.error("", e);
      }
    }
    return newReturnValue;
  }
}
