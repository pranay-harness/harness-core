package io.harness.walktree.visitor.inputset;

import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.common.NGExpressionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.reflection.ReflectionUtils;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldRegistry;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import io.harness.walktree.visitor.DummyVisitableElement;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.walktree.visitor.utilities.VisitorDummyElementUtils;
import io.harness.walktree.visitor.utilities.VisitorReflectionUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

@Slf4j
public class InputSetTemplateVisitor extends SimpleVisitor<DummyVisitableElement> {
  @Inject private VisitorFieldRegistry visitorFieldRegistry;
  @Getter private Object currentObject;

  public InputSetTemplateVisitor(Injector injector) {
    super(injector);
  }

  /*
  Sets up a dummy element and populates those parameter field entries which are input at this node
   */
  @Override
  public VisitElementResult visitElement(Object currentElement) {
    Object dummyElement = getNewDummyObject(currentElement);

    boolean hasInput = findIfCurrentElementHasInputExpression(currentElement, dummyElement);
    VisitorDummyElementUtils.addToDummyElementMapUsingPredicate(
        getElementToDummyElementMap(), currentElement, dummyElement, object -> hasInput);
    return VisitElementResult.CONTINUE;
  }

  /*
  Finds parameter field elements which are input and sets them into the dummy element. Returns whether an input
  was found or not
   */
  boolean findIfCurrentElementHasInputExpression(Object currentElement, Object dummyElement) {
    boolean foundInput = false;
    List<Field> currentElementFields = ReflectionUtils.getAllDeclaredAndInheritedFields(currentElement.getClass());
    for (Field currentElementField : currentElementFields) {
      Object fieldValue = ReflectionUtils.getFieldValue(currentElement, currentElementField);
      if (fieldValue != null && isRuntimeInput(fieldValue)) {
        foundInput = true;
        try {
          VisitorReflectionUtils.addValueToField(dummyElement, currentElementField, fieldValue);
        } catch (IllegalAccessException e) {
          throw new InvalidRequestException("Field could not be added because " + e.getMessage());
        }
      }
    }

    return foundInput;
  }

  /*
  Adds children present in the dummy element map into the dummy element of the current element
  If current element has no input in itself or any of its children, it is not put in the dummy
  element map
   */
  @Override
  public VisitElementResult postVisitElement(Object currentElement) {
    VisitorDummyElementUtils.addChildrenToCurrentDummyElement(currentElement, getElementToDummyElementMap(), this);

    currentObject = VisitorDummyElementUtils.getDummyElementFromMap(getElementToDummyElementMap(), currentElement);
    return super.postVisitElement(currentElement);
  }

  /*
  checks if a particular value is a runtime input or not
   */
  private boolean isRuntimeInput(Object value) {
    if (VisitorFieldWrapper.class.isAssignableFrom(value.getClass())) {
      VisitorFieldWrapper wrapper = (VisitorFieldWrapper) value;
      VisitableFieldProcessor visitableFieldProcessor = visitorFieldRegistry.obtain(wrapper.getVisitorFieldType());
      String expressionValue = visitableFieldProcessor.getExpressionFieldValue(wrapper);
      return NGExpressionUtils.matchesInputSetPattern(expressionValue);
    }
    return false;
  }
}
