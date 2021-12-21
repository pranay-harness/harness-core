package io.harness;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OrganizationConstants {
  public static final String CREATED_AT = "This is the time at which Organization was created.";
  public static final String LAST_MODIFIED_AT = "This is the time at which Organization was last modified.";
  public static final String HARNESS_MANAGED =
      "This indicates if this Organization is managed by Harness or not. If True, Harness can manage and modify this Organization.";
  public static final String NAME = "Name of the Organization";
  public static final String IDENTIFIER = "Identifier of the Organization";
  public static final String SEARCH_TERM = "Text to search/filter the Organizations";
  public static final String IGNORE_CASE =
      "Boolean value to indicate if the case of the searched term should be ignored while filtering";
  public static final String IDENTIFIER_LIST =
      "This is the list of the Organization identifiers on which the filter will be applied.";
}
