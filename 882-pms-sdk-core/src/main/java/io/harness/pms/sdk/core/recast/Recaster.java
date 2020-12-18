package io.harness.pms.sdk.core.recast;

import static io.harness.pms.sdk.core.recast.RecastReflectionUtils.getParameterizedClass;

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
  private final CustomFieldRecaster fieldRecaster;
  private final RecastObjectFactory objectFactory = new RecastObjectCreator();
  private final RecasterOptions options = new RecasterOptions();

  public Recaster() {
    this.transformer = new Transformer(this);
    this.fieldRecaster = new DefaultFieldRecaster(this, transformer);
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
    fieldRecaster.toDocument(entity, cf, document, involvedObjects);
  }
}
