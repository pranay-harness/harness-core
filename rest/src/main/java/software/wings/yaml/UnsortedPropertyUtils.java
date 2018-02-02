package software.wings.yaml;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.BeanAccess;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;

import java.util.LinkedHashSet;
import java.util.Set;

class UnsortedPropertyUtils extends PropertyUtils {
  @Override
  protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess) {
    try {
      return new LinkedHashSet<>(getPropertiesMap(type, BeanAccess.FIELD).values());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }
}