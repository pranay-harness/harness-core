package software.wings.yaml;

import static software.wings.beans.yaml.YamlConstants.FIELD_HARNESS_API_VERSION;
import static software.wings.beans.yaml.YamlConstants.FIELD_TYPE;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.BeanAccess;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.PropertyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.Misc;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

class CustomPropertyUtils extends PropertyUtils {
  private static final Logger logger = LoggerFactory.getLogger(CustomPropertyUtils.class);

  @Override
  protected Set<Property> createPropertySet(Class<? extends Object> type, BeanAccess bAccess) {
    try {
      Collection<Property> values = getPropertiesMap(type, BeanAccess.FIELD).values();
      Set<Property> result = new TreeSet<>(new CustomComparator());
      result.addAll(values);

      return result;
    } catch (Exception e) {
      logger.error("Error while converting the object to yaml string: " + Misc.getMessage(e), e);
    }

    return null;
  }

  @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
  private static class CustomComparator implements Comparator<Property> {
    @Override
    public int compare(Property lhs, Property rhs) {
      if (FIELD_HARNESS_API_VERSION.equals(lhs.getName())) {
        return -1;
      } else if (FIELD_TYPE.equals(lhs.getName())) {
        if (FIELD_HARNESS_API_VERSION.equals(rhs.getName())) {
          return 1;
        } else {
          return -1;
        }
      }

      return StringUtils.compare(lhs.getName(), rhs.getName());
    }
  }
}
