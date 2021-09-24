package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class NGResourceFilterConstants {
  public static final String SEARCH_TERM_KEY = "searchTerm";
  public static final String FILTER_QUERY_KEY = "filterQuery";
  public static final String MODULE_TYPE_KEY = "moduleType";
  public static final String TYPE_KEY = "type";
  public static final String PAGE_KEY = "pageIndex";
  public static final String SIZE_KEY = "pageSize";
  public static final String SORT_KEY = "sortOrders";
  public static final String EMAIL_KEY = "emailId";
  public static final String CASE_INSENSITIVE_MONGO_OPTIONS = "i";
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String STATUS = "status";
  public static final String NAME = "name";
  public static final String IDENTIFIER = "identifier";
  public static final String IDENTIFIERS = "identifiers";
  public static final String DESCRIPTION = "description";
  public static final String FILTER_KEY = "filterIdentifier";
  public static final String SCOPE_KEY = "scope";
  public static final String SCOPE = "scope";
  public static final String TIME_GROUP_BY_TYPE = "timeGroupByType";
  public static final String GROUP_BY = "groupBy";
  public static final String SORT_BY = "sortBy";
}
