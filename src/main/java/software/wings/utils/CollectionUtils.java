package software.wings.utils;

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
 *  Miscellaneous collection utility methods.
 *
 *
 * @author Rishi
 *
 */
public class CollectionUtils {
  /**
   * This method is used to extract specific field values from the list of objects
   * @param cls
   * @param list
   * @param fieldName
   * @return
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   * @throws IntrospectionException
   */
  public static <T> List<T> fields(Class<T> cls, List list, String fieldName)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    if (list == null) {
      return null;
    }
    ArrayList<T> fieldList = new ArrayList<T>();
    if (list.size() == 0) {
      return fieldList;
    }

    Method readMethod = getReadMethod(list.get(0).getClass(), fieldName);
    for (Object obj : list) {
      fieldList.add((T) readMethod.invoke(obj));
    }
    return fieldList;
  }

  /**
   * This method is used to create hierarchy by specific field value from the list of objects
   *
   * @param list
   * @param fieldName
   * @return
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   * @throws IntrospectionException
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
      F fieldValue = (F) readMethod.invoke(obj);
      List<T> objList = map.get(fieldValue);
      if (objList == null) {
        objList = new ArrayList();
        map.put(fieldValue, objList);
      }
      objList.add(obj);
    }
    return map;
  }

  private static Method getReadMethod(Class cls, String fieldName) throws IntrospectionException {
    PropertyDescriptor[] pds = Introspector.getBeanInfo(cls).getPropertyDescriptors();
    for (PropertyDescriptor pd : pds) {
      if (pd.getName().equals(fieldName)) {
        return pd.getReadMethod();
      }
    }
    return null;
  }
}
