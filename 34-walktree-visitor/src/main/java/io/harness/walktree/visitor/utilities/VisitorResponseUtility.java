package io.harness.walktree.visitor.utilities;

import io.harness.data.structure.UUIDGenerator;
import io.harness.reflection.ReflectionUtils;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldRegistry;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.walktree.visitor.WithMetadata;
import io.harness.walktree.visitor.response.VisitorResponse;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.Map;

@UtilityClass
public class VisitorResponseUtility {
  /**
   * Adds the given uuid -> validationErrors to the uuidToErrorMap
   * Appends to the given uuid if the uuid already exists.
   * @param uuid
   * @param validationResponseList
   */
  public void addToVisitorResponse(String uuid, VisitorResponse validationResponseList,
      Map<String, VisitorResponse> uuidToErrorResponseList, VisitorResponse newObject) {
    VisitorResponse existingValidationResponseList = uuidToErrorResponseList.getOrDefault(uuid, newObject);
    existingValidationResponseList.add(validationResponseList);
    uuidToErrorResponseList.put(uuid, validationResponseList);
  }

  public <T> String addUUIDValueToGivenField(SimpleVisitor<T> simpleVisitor, Object element,
      VisitorFieldRegistry visitorFieldRegistry, String fieldName, boolean useFQN) throws IllegalAccessException {
    Field field = ReflectionUtils.getFieldByName(element.getClass(), fieldName);
    field.setAccessible(true);
    if (VisitorFieldWrapper.class.isAssignableFrom(field.getType())) {
      return addWrappedUUID(simpleVisitor, visitorFieldRegistry, element, field, useFQN);
    } else if (field.getType() == String.class) {
      return addUUID(simpleVisitor, element, field, useFQN);
    }
    return addUUIDToMetaDataField(simpleVisitor, element, useFQN);
  }

  public <T> String getUUid(SimpleVisitor<T> simpleVisitor, String fieldName, boolean useFQN) {
    return useFQN
        ? VisitorParentPathUtilities.getFullQualifiedDomainName(simpleVisitor.getContextMap()) + "." + fieldName
        : UUIDGenerator.generateUuid();
  }

  private <T> String addWrappedUUID(SimpleVisitor<T> simpleVisitor, VisitorFieldRegistry visitorFieldRegistry,
      Object element, Field field, boolean usFQN) throws IllegalAccessException {
    if (field.get(element) == null) {
      String uuid = getUUid(simpleVisitor, field.getName(), usFQN);
      VisitableFieldProcessor<?> visitorFieldWrapper =
          visitorFieldRegistry.obtain(VisitorFieldType.builder().type("PARAMETER_FIELD").build());

      field.set(element, visitorFieldWrapper.createNewFieldWithStringValue(uuid));
      return uuid;
    }
    VisitorFieldWrapper parameterField = (VisitorFieldWrapper) field.get(element);
    VisitorFieldType visitorFieldType = visitorFieldRegistry.obtainFieldType(parameterField.getClass());
    VisitableFieldProcessor visitorFieldWrapper = visitorFieldRegistry.obtain(visitorFieldType);
    return visitorFieldWrapper.getFieldWithStringValue(parameterField);
  }

  private <T> String addUUID(SimpleVisitor<T> simpleVisitor, Object element, Field field, boolean usFQN)
      throws IllegalAccessException {
    if (field.get(element) == null) {
      String uuid = getUUid(simpleVisitor, field.getName(), usFQN);
      field.set(element, uuid);
      return uuid;
    }
    return (String) field.get(element);
  }

  private <T> String addUUIDToMetaDataField(SimpleVisitor<T> simpleVisitor, Object element, boolean usFQN)
      throws IllegalAccessException {
    if (element instanceof WithMetadata) {
      Field field = ReflectionUtils.getFieldByName(element.getClass(), "metadata");
      if (field == null) {
        throw new IllegalAccessException();
      }
      field.setAccessible(true);
      if (field.get(element) == null) {
        String uuid = getUUid(simpleVisitor, field.getName(), usFQN);
        field.set(element, uuid);
        return uuid;
      }
      return (String) field.get(element);
    }
    throw new IllegalAccessException();
  }
}
