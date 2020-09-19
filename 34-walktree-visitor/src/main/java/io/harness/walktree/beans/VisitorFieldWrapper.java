package io.harness.walktree.beans;

public interface VisitorFieldWrapper<T> {
  /**
   * This function will return string value for walktree impl for handling nodes having properties of custom java
   * classes like ParameterField.
   * @return String value
   */
  String getStringFieldValue();

  /**
   * This function is used to update current field with required properties of given object.
   * @param object
   * @return T
   */
  T updateCurrentField(T object);

  /**
   * This function is used to create new field of custom java classes.
   */
  T createNewField(String fieldValue);
}
