package io.harness.walktree.visitor.utilities;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.harness.reflection.ReflectionUtils;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.Visitable;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@UtilityClass
public class VisitorDummyElementUtilities {
  /**
   *
   * Adds the dummy child object to the dummy element of the parent.
   * NOTE: Only those child objects will be added that have an entry in the elementToDummyElementMap
   *
   * @param parentElement - the original element which contains visitable children
   * @param parentDummyElement - the object to which we want to add the dummy child objects
   * @param elementToDummyElementMap - the map consisting of original objects to dummy objects. Required for filtering
   *     getting the dummy element corresponding to the child.
   * @return
   */
  public VisitableChildren addDummyChildrenToGivenElement(
      Object parentElement, Object parentDummyElement, Map<Object, Object> elementToDummyElementMap) {
    Multimap<String, Object> fieldNameToDummyObjects =
        getFieldNamesToDummyObjects(parentElement, elementToDummyElementMap);
    if (fieldNameToDummyObjects.isEmpty()) {
      return VisitableChildren.builder().build();
    }
    VisitableChildren unhandledVisitableChildren = VisitableChildren.builder().build();
    for (String key : fieldNameToDummyObjects.keySet()) {
      Field field = ReflectionUtils.getFieldByName(parentDummyElement.getClass(), key);
      if (List.class.isAssignableFrom(field.getType())) {
        try {
          List<Object> childObjects = new ArrayList<>(fieldNameToDummyObjects.get(key));
          addValueToField(parentDummyElement, key, childObjects);
        } catch (Exception e) {
          // add to unhandled children so that we can give control to the VisitorHelper for the current object.
          fieldNameToDummyObjects.values().forEach(value -> unhandledVisitableChildren.add(key, value));
        }
      } else {
        for (Object value : fieldNameToDummyObjects.get(key)) {
          if (Map.class.isAssignableFrom(field.getType())) {
            unhandledVisitableChildren.add(key, value);
          } else {
            try {
              addValueToField(parentDummyElement, key, value);
            } catch (IllegalAccessException e) {
              unhandledVisitableChildren.add(key, value);
            }
          }
        }
      }
    }
    return unhandledVisitableChildren;
  }

  private Multimap<String, Object> getFieldNamesToDummyObjects(
      Object parentElement, Map<Object, Object> elementToDummyElementMap) {
    Multimap<String, Object> fieldNameToDummyObjects = HashMultimap.create();
    getDummyVisitableChildrenFromElementToDummyMap(parentElement, elementToDummyElementMap)
        .getVisitableChildList()
        .forEach(visitableChild
            -> fieldNameToDummyObjects.put(
                visitableChild.getFieldName(), elementToDummyElementMap.get(visitableChild.getValue())));
    return fieldNameToDummyObjects;
  }

  /**
   * Returns the dummy child of the given parentElement which exists in elementToDummyElementMap
   *
   * @param parentElement - the parent element from which we want to extract dummy children.
   * @param elementToDummyElementMap - A map consisting of element to dummy element.
   * @return
   */
  public VisitableChildren getDummyVisitableChildrenFromElementToDummyMap(
      Object parentElement, Map<Object, Object> elementToDummyElementMap) {
    if (parentElement instanceof Visitable) {
      VisitableChildren childrenToWalk = ((Visitable) parentElement).getChildrenToWalk();
      if (childrenToWalk == null) {
        return VisitableChildren.builder().build();
      }
      List<VisitableChild> dummyChildren =
          childrenToWalk.getVisitableChildList()
              .stream()
              .filter(visitableChild -> elementToDummyElementMap.containsKey(visitableChild.getValue()))
              .collect(Collectors.toList());
      return VisitableChildren.builder().visitableChildList(dummyChildren).build();
    }
    return VisitableChildren.builder().build();
  }

  public Field addValueToField(Object element, String fieldName, Object values) throws IllegalAccessException {
    Field field = ReflectionUtils.getFieldByName(element.getClass(), fieldName);
    field.setAccessible(true);
    field.set(element, values);
    return field;
  }

  public void addToDummyElementMapUsingPredicate(
      Map<Object, Object> elementToDummyElementMap, Object key, Object value, Predicate<Object> predicate) {
    if (predicate.test(key)) {
      addToDummyElementMap(elementToDummyElementMap, key, value);
    }
  }

  public void addToDummyElementMap(Map<Object, Object> elementToDummyElementMap, Object key, Object value) {
    if (elementToDummyElementMap.containsKey(key)) {
      return;
    }
    elementToDummyElementMap.put(key, value);
  }

  public Object getDummyElement(Map<Object, Object> elementToDummyElementMap, Object originalObject) {
    return elementToDummyElementMap.get(originalObject);
  }
}
