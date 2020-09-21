package io.harness.walktree.visitor.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.registries.visitorfield.VisitorFieldRegistry;
import io.harness.walktree.visitor.ParentQualifier;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.walktree.visitor.VisitorErrorResponse;
import io.harness.walktree.visitor.VisitorErrorResponseList;
import io.harness.walktree.visitor.response.VisitorResponse;
import io.harness.walktree.visitor.utilities.VisitorDummyElementUtilities;
import io.harness.walktree.visitor.utilities.VisitorParentPathUtilities;
import io.harness.walktree.visitor.utilities.VisitorResponseUtility;
import io.harness.walktree.visitor.validation.modes.ModeType;
import io.harness.walktree.visitor.validation.modes.PostInputSet;
import io.harness.walktree.visitor.validation.modes.PreInputSet;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@Slf4j
public class ValidationVisitor extends SimpleVisitor<ConfigValidator> {
  @Inject private VisitorFieldRegistry visitorFieldRegistry;

  private final boolean useFQN;
  private final Class<?> modeType;
  @Getter private Object currentObject;

  @Getter Map<String, VisitorResponse> uuidToVisitorResponse = new HashMap<>();

  public ValidationVisitor(Injector injector, Class<?> modeType, boolean useFQN) {
    super(injector);
    this.modeType = modeType;
    this.useFQN = useFQN;
  }

  @Override
  public VisitElementResult preVisitElement(Object element) {
    // add to the list of parent.
    if (element instanceof ParentQualifier) {
      LevelNode levelNode = ((ParentQualifier) element).getLevelNode();
      VisitorParentPathUtilities.addToParentList(this.getContextMap(), levelNode);
    }
    return VisitElementResult.CONTINUE;
  }

  @Override
  public VisitElementResult postVisitElement(Object element) {
    addErrorChildrenToCurrentElement(element);
    // Remove from parent list once traversed
    if (element instanceof ParentQualifier) {
      VisitorParentPathUtilities.removeFromParentList(this.getContextMap());
    }
    currentObject = VisitorDummyElementUtilities.getDummyElement(getElementToDummyElementMap(), element);
    return super.postVisitElement(element);
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    ConfigValidator helperClass = getHelperClass(currentElement);
    Object dummyElement;
    if (helperClass != null) {
      dummyElement = helperClass.createDummyVisitableElement(currentElement);
    } else {
      logger.error("Helper Class not implemented for object of type" + currentElement.getClass());
      throw new NotImplementedException("Helper Class not implemented for object of type" + currentElement.getClass());
    }
    boolean hasError = validateAnnotations(currentElement, dummyElement);
    VisitorDummyElementUtilities.addToDummyElementMapUsingPredicate(
        getElementToDummyElementMap(), currentElement, dummyElement, object -> hasError);

    helperClass.validate(currentElement, this);
    return VisitElementResult.CONTINUE;
  }

  /**
   * Used to validate the fields of the given object using
   * @param currentElement -  the original element on which validation to work
   * @param dummyElement - the dummy element returned by HelperClass#getDummyObject.
   * @return
   */
  @VisibleForTesting
  boolean validateAnnotations(Object currentElement, Object dummyElement) {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    Set<ConstraintViolation<Object>> violations = new HashSet<>();
    if (modeType == ModeType.POST_INPUT_SET) {
      violations.addAll(validator.validate(currentElement, PostInputSet.class));
    } else {
      violations.addAll(validator.validate(currentElement, PreInputSet.class));
    }
    if (!violations.isEmpty()) {
      generateUUIDAndAddToVisitorResponse(violations, dummyElement);
      return true;
    }
    return false;
  }

  private void generateUUIDAndAddToVisitorResponse(Set<ConstraintViolation<Object>> violations, Object dummyElement) {
    Multimap<String, String> errorsMap = ArrayListMultimap.create();
    violations.forEach(objectConstraintViolation
        -> errorsMap.put(
            objectConstraintViolation.getPropertyPath().toString(), objectConstraintViolation.getMessage()));
    for (String key : errorsMap.keys()) {
      try {
        List<String> errorMessages = new ArrayList<>(errorsMap.get(key));
        VisitorErrorResponseList visitorErrorResponseList =
            VisitorErrorResponseList.builder()
                .errors(errorMessages.stream()
                            .map(message -> VisitorErrorResponse.builder().fieldName(key).message(message).build())
                            .collect(Collectors.toList()))
                .build();
        String uuid =
            VisitorResponseUtility.addUUIDValueToGivenField(this, dummyElement, visitorFieldRegistry, key, useFQN);
        VisitorResponseUtility.addToVisitorResponse(
            uuid, visitorErrorResponseList, uuidToVisitorResponse, VisitorErrorResponseList.builder().build());

      } catch (IllegalAccessException e) {
        logger.error(String.format("Error using reflection : %s", e.getMessage()));
      }
    }
  }

  /**
   * Adds the child to the currentDummyElement.
   * Handles only user-defined objects. It does not handle collections or Maps type for now.
   */
  @VisibleForTesting
  void addErrorChildrenToCurrentElement(Object element) {
    // If element has children and they have errors then we want to merge them to dummy object for this element.
    VisitableChildren dummyVisitableChildrenFromElementToDummyMap =
        VisitorDummyElementUtilities.getDummyVisitableChildrenFromElementToDummyMap(
            element, getElementToDummyElementMap());
    if (dummyVisitableChildrenFromElementToDummyMap.isEmpty()) {
      return;
    }
    ConfigValidator helperClass = getHelperClass(element);
    if (helperClass != null) {
      VisitorDummyElementUtilities.addToDummyElementMap(
          getElementToDummyElementMap(), element, helperClass.createDummyVisitableElement(element));
      VisitableChildren visitableChildren = VisitorDummyElementUtilities.addDummyChildrenToGivenElement(element,
          VisitorDummyElementUtilities.getDummyElement(getElementToDummyElementMap(), element),
          getElementToDummyElementMap());
      helperClass.handleComplexVisitableChildren(
          VisitorDummyElementUtilities.getDummyElement(getElementToDummyElementMap(), element), this,
          visitableChildren);
    }
  }
}
