package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.AND;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SearchFilter.Operator.OR;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.HarnessTagFilter;
import software.wings.beans.HarnessTagFilter.TagFilterCondition;
import software.wings.beans.ResourceLookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class HarnessTagFilterHelper {
  private static final String TAGS_NAME = "tags.name";
  private static final String TAGS_VALUE = "tags.value";
  private static final String QUERY_KEY = "query";

  public void addHarnessTagFiltersToPageRequest(PageRequest<ResourceLookup> pageRequest, String filter) {
    HarnessTagFilter harnessTagFilter = convertToHarnessTagFilter(filter);
    if (harnessTagFilter == null || isEmpty(harnessTagFilter.getConditions())) {
      return;
    }

    List<SearchFilter> searchFilters = new ArrayList<>();
    for (TagFilterCondition tagFilterCondition : harnessTagFilter.getConditions()) {
      SearchFilter nameSearchFilter = prepareSearchFilter(TAGS_NAME, EQ, new Object[] {tagFilterCondition.getName()});
      SearchFilter valueSearchFilter = prepareSearchFilter(TAGS_VALUE, IN, tagFilterCondition.getValues().toArray());

      searchFilters.add(prepareSearchFilter(QUERY_KEY, AND, new Object[] {nameSearchFilter, valueSearchFilter}));
    }

    pageRequest.addFilter(
        prepareSearchFilter(QUERY_KEY, harnessTagFilter.isMatchAll() ? AND : OR, searchFilters.toArray()));
  }

  private SearchFilter prepareSearchFilter(String fieldName, Operator op, Object[] fieldValues) {
    return SearchFilter.builder().fieldName(fieldName).op(op).fieldValues(fieldValues).build();
  }

  private HarnessTagFilter convertToHarnessTagFilter(String filter) {
    if (isBlank(filter)) {
      return null;
    }

    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    try {
      return mapper.readValue(filter, HarnessTagFilter.class);
    } catch (IOException e) {
      logger.info("Exception " + e);
      String errorMsg = "Failed to deserialize json into HarnessTagFilter";
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }
}
