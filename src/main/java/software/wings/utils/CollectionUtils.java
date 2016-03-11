package software.wings.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.reflect.FieldUtils;

public class CollectionUtils {
  public static <T> List<T> fields(Class<T> cls, List list, String fieldName) throws IllegalAccessException {
    if (list == null) {
      return null;
    }
    ArrayList<T> fieldList = new ArrayList<T>();
    if (list.size() == 0) {
      return fieldList;
    }

    Field field = FieldUtils.getField(list.get(0).getClass(), fieldName);
    for (Object obj : list) {
      fieldList.add((T) field.get(obj));
    }
    return fieldList;
  }

  public static <T> Map<Object, List<T>> hierarchy(List<T> list, String fieldName) throws IllegalAccessException {
    if (list == null || fieldName == null) {
      return null;
    }
    Map<Object, List<T>> map = new HashMap<>();
    if (list.size() == 0) {
      return map;
    }
    Field field = FieldUtils.getField(list.get(0).getClass(), fieldName);
    for (T obj : list) {
      Object fieldValue = field.get(obj);
      List<T> objList = map.get(fieldValue);
      if (objList == null) {
        objList = new ArrayList();
        map.put(fieldValue, objList);
      }
      objList.add(obj);
    }
    return map;
  }
}
