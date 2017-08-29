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
      Set<Property> result = new LinkedHashSet<Property>(getPropertiesMap(type, BeanAccess.FIELD).values());
      // We don't need this
      // result.remove(result.iterator().next());  // drop 'listInt' property
      return result;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }
}