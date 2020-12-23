package io.harness.core;

import static java.lang.String.format;

import io.harness.beans.CastedClass;
import io.harness.beans.CastedField;
import io.harness.exceptions.RecasterException;
import io.harness.fieldrecaster.DefaultFieldRecaster;
import io.harness.fieldrecaster.FieldRecaster;
import io.harness.fieldrecaster.SimpleValueFieldRecaster;

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

  private final Map<String, CastedClass> castedClasses = new ConcurrentHashMap<>();
  private final Transformer transformer;
  private final FieldRecaster defaultFieldRecaster;
  private final FieldRecaster simpleValueFieldRecaster;
  private final RecastObjectFactory objectFactory = new RecastObjectCreator();
  private final RecasterOptions options = new RecasterOptions();

  public Recaster() {
    this.transformer = new Transformer(this);
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
    Class type = (obj instanceof Class) ? (Class) obj : obj.getClass();
    CastedClass cc = castedClasses.get(type.getName());
    if (cc == null) {
      cc = new CastedClass(type, this);
      castedClasses.put(cc.getClazz().getName(), cc);
    }
    return cc;
  }

  public <T> T fromDocument(final Document document, final Class<T> entityClass) {
    if (document == null) {
      log.warn("Null reference was passed in document");
      return null;
    }
    T entity;
    entity = objectFactory.createInstance(entityClass, document);
    entity = fromDocument(document, entity);
    return entity;
  }

  public <T> T fromDocument(final Document document, T entity) {
    if (entity instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) entity;
      for (String key : document.keySet()) {
        Object o = document.get(key);
        map.put(key, (o instanceof Document) ? fromDocument((Document) o) : o);
      }
    } else if (entity instanceof Collection) {
      Collection<Object> collection = (Collection<Object>) entity;
      for (Object o : (List) document) {
        collection.add((o instanceof Document) ? fromDocument((Document) o) : o);
      }
    } else {
      final CastedClass castedClass = getCastedClass(entity);
      try {
        for (final CastedField cf : castedClass.getPersistenceFields()) {
          readCastedField(document, cf, entity);
        }
      } catch (final Exception e1) {
        // TODO(Alexei) define custom exception
        log.error("Cannot map [{}] to [{}] class", document.get(RECAST_CLASS_KEY), entity.getClass(), e1);
        throw new RuntimeException();
      }
    }

    return entity;
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
    Document document = new Document();
    final CastedClass cc = getCastedClass(entity);
    document.put(RECAST_CLASS_KEY, entity.getClass().getName());

    for (final CastedField cf : cc.getPersistenceFields()) {
      try {
        writeCastedField(entity, cf, document, involvedObjects);
      } catch (Exception e) {
        throw new RecasterException("Error mapping field:" + cf.getFullName(), e);
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
