package io.harness.core;

import static java.lang.String.format;

import io.harness.beans.CastedClass;
import io.harness.beans.CastedField;
import io.harness.exceptions.CastedFieldException;
import io.harness.exceptions.RecasterException;
import io.harness.fieldrecaster.DefaultFieldRecaster;
import io.harness.fieldrecaster.FieldRecaster;
import io.harness.fieldrecaster.SimpleValueFieldRecaster;

import com.google.protobuf.Message;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Getter
@Slf4j
public class Recaster {
  public static final String RECAST_CLASS_KEY = "__recast";
  public static final String ENCODED_VALUE = "__encodedValue";

  private final Map<String, CastedClass> castedClasses = new ConcurrentHashMap<>();
  private final Transformer transformer;
  private final FieldRecaster defaultFieldRecaster;
  private final FieldRecaster simpleValueFieldRecaster;
  private final RecastObjectFactory objectFactory = new RecastObjectCreator();
  private final RecasterOptions options = new RecasterOptions();

  public Recaster() {
    this.transformer = new CustomTransformer(this);
    this.defaultFieldRecaster = new DefaultFieldRecaster();
    this.simpleValueFieldRecaster = new SimpleValueFieldRecaster();
  }

  public boolean isCasted(Class<?> entityClass) {
    return castedClasses.containsKey(entityClass.getName());
  }

  public CastedClass addCastedClass(Class<?> entityClass) {
    CastedClass castedClass = castedClasses.get(entityClass.getName());
    if (castedClass == null) {
      castedClass = new CastedClass(entityClass, this);
      castedClasses.put(castedClass.getClazz().getName(), castedClass);
    }
    return castedClass;
  }

  public CastedClass getCastedClass(Object obj) {
    if (obj == null) {
      return null;
    }
    Class<?> type = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
    CastedClass cc = castedClasses.get(type.getName());
    if (cc == null) {
      cc = new CastedClass(type, this);
      castedClasses.put(cc.getClazz().getName(), cc);
    }
    return cc;
  }

  @SuppressWarnings("unchecked")
  public <T> T fromDocument(final Document document, final Class<T> entityClass) {
    if (document == null) {
      log.warn("Null reference was passed in document");
      return null;
    }

    if (!document.containsKey(RECAST_CLASS_KEY)) {
      throw new RecasterException(
          format("The document does not contain a %s key. Determining entity type is impossible.", RECAST_CLASS_KEY));
    }

    if (transformer.hasTransformer(entityClass)) {
      return (T) transformer.decode(entityClass, document.get(ENCODED_VALUE), null);
    }

    T entity;
    entity = objectFactory.createInstance(entityClass, document);

    if (!entityClass.isAssignableFrom(entity.getClass())) {
      throw new RecasterException(
          format("%s class cannot be mapped to %s", document.get(RECAST_CLASS_KEY), entityClass.getName()));
    }

    entity = fromDocument(document, entity);
    return entity;
  }

  @SuppressWarnings("unchecked")
  public <T> T fromDocument(final Document document, T entity) {
    if (entity instanceof Message) {
      return decodeProto(document, entity);
    }
    if (entity instanceof Map) {
      populateMap(document, entity);
    } else if (entity instanceof Collection) {
      populateCollection(document, entity);
    } else {
      final CastedClass castedClass = getCastedClass(entity);
      for (final CastedField cf : castedClass.getPersistenceFields()) {
        try {
          readCastedField(document, cf, entity);
        } catch (final Exception e) {
          log.error("Cannot map [{}] to [{}] class", document.get(RECAST_CLASS_KEY), entity.getClass(), e);
          throw new CastedFieldException(
              format("Cannot map [%s] to [%s] class for field [%s]", document.get(RECAST_CLASS_KEY), entity.getClass(),
                  cf.getField().getName()),
              e);
        }
      }
    }

    return entity;
  }

  @SuppressWarnings("unchecked")
  private <T> T decodeProto(final Document document, T entity) {
    if (!document.containsKey(ENCODED_VALUE)) {
      throw new RecasterException(
          format("The proto document does not contain a %s key. Decoding proto is impossible.", ENCODED_VALUE));
    }
    return (T) transformer.decode(entity.getClass(), document.get(ENCODED_VALUE), null);
  }

  @SuppressWarnings("unchecked")
  private <T> void populateMap(final Document document, T entity) {
    Map<String, Object> map = (Map<String, Object>) entity;
    for (Map.Entry<String, Object> entry : document.entrySet()) {
      if (entry.getKey().equals(RECAST_CLASS_KEY)) {
        continue;
      }
      Object o = document.get(entry.getKey());
      map.put(entry.getKey(), (o instanceof Document) ? fromDocument((Document) o) : o);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void populateCollection(final Document document, T entity) {
    Collection<Object> collection = (Collection<Object>) entity;
    for (Object o : (List<Object>) document) {
      collection.add((o instanceof Document) ? fromDocument((Document) o) : o);
    }
  }

  private <T> T fromDocument(final Document document) {
    if (document.containsKey(RECAST_CLASS_KEY)) {
      T entity = getObjectFactory().createInstance(null, document);
      entity = fromDocument(document, entity);

      return entity;
    } else {
      throw new RecasterException(
          format("The document does not contain a %s key. Determining entity type is impossible.", RECAST_CLASS_KEY));
    }
  }

  private void readCastedField(final Document document, final CastedField cf, final Object entity) {
    // Annotation logic should be wired here
    if (transformer.hasSimpleValueTransformer(cf)) {
      simpleValueFieldRecaster.fromDocument(this, document, cf, entity);
    } else {
      defaultFieldRecaster.fromDocument(this, document, cf, entity);
    }
  }

  public Document toDocument(final Object entity) {
    return toDocument(entity, null);
  }

  public Document toDocument(Object entity, final Map<Object, Document> involvedObjects) {
    if (entity == null) {
      log.warn("Null reference was passed as object");
      return null;
    }

    if (entity instanceof Document) {
      return (Document) entity;
    }

    Document document = new Document();
    final CastedClass cc = getCastedClass(entity);
    document.put(RECAST_CLASS_KEY, entity.getClass().getName());

    if (transformer.hasTransformer(entity.getClass())) {
      Object encode = transformer.encode(entity);
      return document.append(ENCODED_VALUE, encode);
    }

    if (entity instanceof Message) {
      Object encodedProto = transformer.encode(entity);
      return document.append(ENCODED_VALUE, encodedProto);
    }

    if (entity instanceof Map) {
      Object encoded = transformer.getTransformer(entity.getClass()).encode(entity);
      if (encoded != null && !(encoded instanceof Document)) {
        throw new RecasterException(format("Cannot transform %s to document", entity.getClass()));
      }
      document.putAll((Document) encoded);
      return document;
    }

    for (final CastedField cf : cc.getPersistenceFields()) {
      try {
        writeCastedField(entity, cf, document, involvedObjects);
      } catch (Exception e) {
        throw new CastedFieldException(format("Cannot map [%s] to [%s] class for field [%s]",
            document.get(RECAST_CLASS_KEY), entity.getClass(), cf.getField().getName()));
      }
    }

    if (involvedObjects != null) {
      involvedObjects.put(entity, document);
    }

    return document;
  }

  private void writeCastedField(
      Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    // Annotation logic should be wired here
    if (transformer.hasSimpleValueTransformer(cf) || transformer.hasSimpleValueTransformer(entity)
        || transformer.hasSimpleValueTransformer(cf.getType())) {
      simpleValueFieldRecaster.toDocument(this, entity, cf, document, involvedObjects);
    } else {
      defaultFieldRecaster.toDocument(this, entity, cf, document, involvedObjects);
    }
  }
}
