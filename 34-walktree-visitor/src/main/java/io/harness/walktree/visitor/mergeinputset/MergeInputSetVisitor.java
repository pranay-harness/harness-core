package io.harness.walktree.visitor.mergeinputset;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.reflection.ReflectionUtils;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.ParentQualifier;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldRegistry;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import io.harness.walktree.visitor.DummyVisitableElement;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.walktree.visitor.response.VisitorResponse;
import io.harness.walktree.visitor.utilities.VisitorDummyElementUtils;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtils;
import io.harness.walktree.visitor.utilities.VisitorReflectionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

@Slf4j
public class MergeInputSetVisitor extends SimpleVisitor<DummyVisitableElement> {
  @Inject private VisitorFieldRegistry visitorFieldRegistry;

  private final boolean useFQN;

  // This map is used to store merge response and its children.
  private final Map<Object, Object> elementToResponseDummyElementMap = new HashMap<>();
  @Getter private Object currentObjectResult;

  // This map is used to traverse input sets parallel to original object.
  private final Map<Object, List<Object>> elementToInputSetElementMap = new HashMap<>();
  private final List<Object> inputSetsList;

  // This is identify if there are errors while merging.
  @Getter private boolean isValidInputSet;
  @Getter private Object currentObjectErrorResult;
  @Getter private final Map<String, VisitorResponse> uuidToVisitorResponse = new HashMap<>();

  public MergeInputSetVisitor(Injector injector, boolean useFQN, List<Object> inputSetsPipeline) {
    super(injector);
    this.useFQN = useFQN;
    this.inputSetsList = inputSetsPipeline;
  }

  @Override
  public VisitElementResult preVisitElement(Object element) {
    // add to the list of parent.
    if (element instanceof ParentQualifier) {
      LevelNode levelNode = ((ParentQualifier) element).getLevelNode();
      VisitorParentPathUtils.addToParentList(this.getContextMap(), levelNode);
    }
    // This is called to initialise elementToInputSetElementMap.
    if (!elementToInputSetElementMap.containsKey(element)) {
      if (!inputSetsList.isEmpty() && checkIfInputSetFieldAtSameLevelOfOriginalField(element, inputSetsList.get(0))) {
        elementToInputSetElementMap.put(element, inputSetsList);
      }
    }
    return super.preVisitElement(element);
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    Object dummyElement = getNewDummyObject(currentElement);
    mergeInputSets(currentElement, dummyElement);
    // Always true predicate
    VisitorDummyElementUtils.addToDummyElementMapUsingPredicate(
        elementToResponseDummyElementMap, currentElement, dummyElement, x -> true);
    return VisitElementResult.CONTINUE;
  }

  @Override
  public VisitElementResult postVisitElement(Object element) {
    addChildrenResponseToCurrentElement(element);

    // Remove from parent list once traversed
    if (element instanceof ParentQualifier) {
      VisitorParentPathUtils.removeFromParentList(this.getContextMap());
    }
    currentObjectResult = VisitorDummyElementUtils.getDummyElementFromMap(elementToResponseDummyElementMap, element);
    return super.postVisitElement(element);
  }

  /**
   * Used to validate the fields of the given object using
   * @param currentElement -  the original element of the original pipeline on which validation to work
   */
  @VisibleForTesting
  void mergeInputSets(Object currentElement, Object dummyElement) {
    List<Field> currentElementFields = ReflectionUtils.getAllDeclaredAndInheritedFields(currentElement.getClass());
    Set<String> currentElementChildren = VisitorDummyElementUtils.getChildrenFieldNames(currentElement);

    // This loop checks only leaf properties which are either String or VisitorFieldWrapper, as they can be runtime
    // expressions i.e ${input}
    try {
      for (Field field : currentElementFields) {
        Object fieldValue = ReflectionUtils.getFieldValue(currentElement, field);

        if (fieldValue != null) {
          if (VisitorFieldWrapper.class.isAssignableFrom(field.getType())) {
            addFieldValueForVisitableFieldWrapper(currentElement, dummyElement, field, fieldValue);
          } else if (field.getType() == String.class) {
            addFieldValueForStringField(currentElement, dummyElement, field, fieldValue);
          } else if (field.getType().isPrimitive() || field.getType().isEnum()) {
            addFieldValueForEnumOrPrimitiveType(dummyElement, field, fieldValue);
          }
        }

        // Add inputSet currentElementChildren fields to map.
        addToWalkInputSetMap(currentElementChildren, field, currentElement);
      }
    } catch (IllegalAccessException e) {
      throw new InvalidArgumentsException(String.format("Error using reflection : %s", e.getMessage()));
    }
  }

  /**
   * This function addsValue to dummyElement when field is of type VisitableFieldWrapper.class
   */
  private void addFieldValueForVisitableFieldWrapper(
      Object currentElement, Object dummyElement, Field field, Object fieldValue) throws IllegalAccessException {
    VisitorFieldWrapper fieldWrapper = (VisitorFieldWrapper) fieldValue;
    VisitableFieldProcessor fieldProcessor = visitorFieldRegistry.obtain(fieldWrapper.getVisitorFieldType());

    // Null check
    if (fieldProcessor.isNull(fieldWrapper)) {
      return;
    }
    String expressionFieldValue = fieldProcessor.getExpressionFieldValue(fieldWrapper);
    VisitorFieldWrapper clonedField = fieldProcessor.cloneField(fieldWrapper);
    if (NGExpressionUtils.matchesInputSetPattern(expressionFieldValue)) {
      Object finalInputSetValue =
          getFinalInputSetValue(field, currentElement, e -> fieldProcessor.isNull((VisitorFieldWrapper) e));
      if (finalInputSetValue != null) {
        fieldProcessor.updateCurrentField(clonedField, (VisitorFieldWrapper) finalInputSetValue);
      }
    }
    VisitorReflectionUtils.addValueToField(dummyElement, field, clonedField);
  }

  /**
   * This function addsValue to dummyElement when field is of type String.class.
   */
  private void addFieldValueForStringField(Object currentElement, Object dummyElement, Field field, Object fieldValue)
      throws IllegalAccessException {
    String stringFieldValue = (String) fieldValue;
    Object finalInputSetValue = fieldValue;
    if (NGExpressionUtils.matchesInputSetPattern(stringFieldValue)) {
      Object appliedInputSetValue = getFinalInputSetValue(field, currentElement, Objects::isNull);
      if (appliedInputSetValue != null) {
        finalInputSetValue = appliedInputSetValue;
      }
    }
    VisitorReflectionUtils.addValueToField(dummyElement, field, finalInputSetValue);
  }

  /**
   * This functions addsValue to dummyElement when field is of type Enum class.
   */
  private void addFieldValueForEnumOrPrimitiveType(Object dummyElement, Field field, Object fieldValue)
      throws IllegalAccessException {
    VisitorReflectionUtils.addValueToField(dummyElement, field, fieldValue);
  }

  /**
   * This functions returns the override value from the inputSetElement corresponding to the current element.
   * @param nullCheckPredicate this checks for null condition for wrapper classes, which are not primitive.
   */
  private Object getFinalInputSetValue(Field field, Object currentElement, Predicate<Object> nullCheckPredicate) {
    List<Object> inputSetList = elementToInputSetElementMap.get(currentElement);
    // If key doesn't exist, then return.
    if (inputSetList == null) {
      return null;
    }
    Object finalValue = null;
    for (Object inputSetElement : inputSetList) {
      if (inputSetElement != null) {
        Object inputSetFieldValue = ReflectionUtils.getFieldValue(inputSetElement, field);
        if (inputSetFieldValue != null && !nullCheckPredicate.test(inputSetFieldValue)) {
          finalValue = inputSetFieldValue;
        }
      }
    }
    return finalValue;
  }

  /**
   * This function adds corresponding inputSetElement fields to #elementToInputSetElementMap for accessing
   * when children are being visited.
   */
  private void addToWalkInputSetMap(Set<String> currentElementChildren, Field field, Object currentElement)
      throws IllegalAccessException {
    Object originalFieldValue = ReflectionUtils.getFieldValue(currentElement, field);
    if (originalFieldValue != null && currentElementChildren.contains(field.getName())) {
      List<Object> inputSetElementsList = elementToInputSetElementMap.get(currentElement);
      // If key doesn't exist, then return.
      if (inputSetElementsList == null) {
        return;
      }
      if (originalFieldValue instanceof List) {
        Map<Object, List<Object>> objectListMap = findChildrenInsideInputSetElementsWithListType(
            inputSetElementsList, field, (List<Object>) originalFieldValue);
        elementToInputSetElementMap.putAll(objectListMap);
      } else {
        List<Object> childrenInsideInputSetElementList =
            findChildrenInsideInputSetElements(inputSetElementsList, field, originalFieldValue);
        elementToInputSetElementMap.put(originalFieldValue, childrenInsideInputSetElementList);
      }
    }
  }

  /**
   * This function finds the corresponding children inside inputSetElementList which corresponds to
   * originalFieldValue, when originalFieldValue is a List.
   */
  private Map<Object, List<Object>> findChildrenInsideInputSetElementsWithListType(
      List<Object> inputSetElementList, Field field, List<Object> originalFieldValuesList) {
    // This map contains input set field elements which are corresponding to originalFieldValue.
    Map<Object, List<Object>> originalFieldToInputSetFieldMap = new HashMap<>();

    Map<Object, Object> dummyFieldToOriginalFieldMap = new HashMap<>();
    for (Object originalFieldValue : originalFieldValuesList) {
      dummyFieldToOriginalFieldMap.put(getNewDummyObject(originalFieldValue), originalFieldValue);
    }

    // This checks for those field values in each input set element whose dummy element is
    // equal to dummy element of originalFieldValue.
    for (Object inputSetElement : inputSetElementList) {
      Object inputSetFieldList = ReflectionUtils.getFieldValue(inputSetElement, field);
      if (inputSetFieldList != null) {
        if (inputSetFieldList instanceof List) {
          List<Object> fieldValuesList = (List<Object>) inputSetFieldList;
          for (Object inputSetField : fieldValuesList) {
            Object inputSetFieldDummy = getNewDummyObject(inputSetField);
            if (dummyFieldToOriginalFieldMap.containsKey(inputSetFieldDummy)) {
              Object key = dummyFieldToOriginalFieldMap.get(inputSetFieldDummy);
              VisitorDummyElementUtils.addToMapWithValuesList(originalFieldToInputSetFieldMap, key, inputSetField);
            }
          }
        } else {
          String errorMessage = "Field type is not matching with original field type.";
          logger.error(errorMessage);
          throw new InvalidArgumentsException(errorMessage);
        }
      }
    }
    return originalFieldToInputSetFieldMap;
  }

  /**
   * This function finds the corresponding children inside inputSetElements which corresponds to originalFieldValue,
   * when originalFieldValue is not a List
   * @return List of child fields of inputSetElements corresponding to originalFieldValue.
   */
  private List<Object> findChildrenInsideInputSetElements(
      List<Object> inputSetElementList, Field field, Object originalFieldValue) {
    // This list contains input set field elements which are corresponding to originalFieldValue.
    List<Object> inputSetFieldElementsResult = new LinkedList<>();

    // This checks for those field values in each input set element whose dummy element is
    // equal to dummy element of originalFieldValue.
    for (Object inputSetElement : inputSetElementList) {
      Object inputSetFieldValue = ReflectionUtils.getFieldValue(inputSetElement, field);
      if (inputSetFieldValue != null) {
        if (checkIfInputSetFieldAtSameLevelOfOriginalField(originalFieldValue, inputSetFieldValue)) {
          inputSetFieldElementsResult.add(inputSetFieldValue);
        }
      }
    }
    return inputSetFieldElementsResult;
  }

  /**
   * This function checks whether inputSetField is corresponding to currentElementField in walking the tree.
   */
  boolean checkIfInputSetFieldAtSameLevelOfOriginalField(Object currentElementField, Object inputSetField) {
    Object inputSetFieldDummy = getNewDummyObject(inputSetField);
    Object originalFieldDummy = getNewDummyObject(currentElementField);
    return inputSetFieldDummy.equals(originalFieldDummy);
  }

  /**
   * This functions adds children of the current element to the currentDummyElement from the given map.
   */
  void addChildrenResponseToCurrentElement(Object currentElement) {
    VisitorDummyElementUtils.addChildrenToCurrentDummyElement(currentElement, elementToResponseDummyElementMap, this);
  }
}
