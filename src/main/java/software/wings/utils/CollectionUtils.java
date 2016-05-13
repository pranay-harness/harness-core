package software.wings.utils;

import software.wings.exception.WingsException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Miscellaneous collection utility methods.
 *
 * @author Rishi
 */
public class CollectionUtils {
  private static Method getReadMethod(Class cls, String fieldName) throws IntrospectionException {
    PropertyDescriptor[] pds = Introspector.getBeanInfo(cls).getPropertyDescriptors();
    for (PropertyDescriptor pd : pds) {
      if (pd.getName().equals(fieldName)) {
        return pd.getReadMethod();
      }
    }
    return null;
  }

  /**
   * This method is used to create hierarchy by specific field value from the list of objects.
   */
  public static <T, F> Map<F, T> hierarchyOnUniqueFieldValue(List<T> list, String fieldName)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<F, List<T>> map = hierarchy(list, fieldName);
    Map<F, T> mapByUniqueValue = new HashMap<>();
    for (F key : map.keySet()) {
      List<T> listT = map.get(key);
      if (listT == null || listT.size() != 1) {
        throw new WingsException("Non-unique fieldValue: field=" + key + ", values=" + listT);
      }
      mapByUniqueValue.put(key, listT.get(0));
    }
    return mapByUniqueValue;
  }

  /**
   * This method is used to create hierarchy by specific field value from the list of objects.
   */
  public static <T, F> Map<F, List<T>> hierarchy(List<T> list, String fieldName)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    if (list == null || fieldName == null) {
      return null;
    }
    Map<F, List<T>> map = new HashMap<>();
    if (list.size() == 0) {
      return map;
    }
    Method readMethod = getReadMethod(list.get(0).getClass(), fieldName);
    for (T obj : list) {
      @SuppressWarnings("unchecked") F fieldValue = (F) readMethod.invoke(obj);
      List<T> objList = map.get(fieldValue);
      if (objList == null) {
        objList = new ArrayList<T>();
        map.put(fieldValue, objList);
      }
      objList.add(obj);
    }
    return map;
  }
}
