package software.wings.graphql.datafetcher;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.ReportTarget.GRAPHQL_API;
import static io.harness.exception.WingsException.USER_SRE;
import static software.wings.graphql.datafetcher.DataFetcherUtils.EXCEPTION_MSG_DELIMITER;
import static software.wings.graphql.datafetcher.DataFetcherUtils.GENERIC_EXCEPTION_MSG;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_LIMIT_ARG_MSG;
import static software.wings.graphql.datafetcher.DataFetcherUtils.NEGATIVE_OFFSET_ARG_MSG;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.directive.DataFetcherDirective.DataFetcherDirectiveAttributes;
import software.wings.graphql.schema.query.QLPageQueryParameterImpl;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLFilterType;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilterType;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilterType;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractConnectionV2DataFetcher<F, S, O> implements DataFetcher {
  public static final String FILTERS = "filters";
  private static final String SORT_CRITERIA = "sortCriteria";
  private static final String LIMIT = "limit";
  private static final String OFFSET = "offset";

  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected DataFetcherUtils utils;

  private final Map<String, DataFetcherDirectiveAttributes> parentToContextFieldArgsMap;

  public AbstractConnectionV2DataFetcher() {
    parentToContextFieldArgsMap = Maps.newHashMap();
  }

  public void addDataFetcherDirectiveAttributesForParent(
      String parentTypeName, DataFetcherDirectiveAttributes dataFetcherDirectiveAttributes) {
    parentToContextFieldArgsMap.putIfAbsent(parentTypeName, dataFetcherDirectiveAttributes);
  }

  @Override
  public Object get(DataFetchingEnvironment environment) throws Exception {
    try {
      Type[] typeArguments = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
      Class<F> filterClass = (Class<F>) typeArguments[0];
      Class<S> sortClass = (Class<S>) typeArguments[1];
      Class<O> returnClass = (Class<O>) typeArguments[2];
      List<F> filters = (List<F>) fetchObject(environment, FILTERS, filterClass);
      final List<S> sort = (List<S>) fetchObject(environment, SORT_CRITERIA, sortClass);
      QLPageQueryParameters pageQueryParameters = extractPageQueryParameters(environment);
      Map<String, String> contextFieldArgsMap =
          utils.getContextFieldArgsMap(parentToContextFieldArgsMap, environment.getParentType().getName());
      if (contextFieldArgsMap != null) {
        if (filters == null) {
          filters = new ArrayList();
        }
        List<F> finalFilters = filters;
        contextFieldArgsMap.forEach((key, value) -> {
          F filter = generateFilter(environment, filterClass, key, value);
          finalFilters.add(filter);
        });
        filters.addAll(finalFilters);
      }

      String parentTypeName = environment.getParentType().getName();
      final String accountId = getAccountId(environment);
      AccountThreadLocal.set(accountId);
      return fetch(filters, pageQueryParameters, sort);
    } catch (WingsException ex) {
      throw new WingsException(getCombinedErrorMessages(ex), ex, ex.getReportTargets());
    } catch (Exception ex) {
      throw new WingsException(GENERIC_EXCEPTION_MSG, USER_SRE);
    } finally {
      AccountThreadLocal.unset();
    }
  }

  private String getCombinedErrorMessages(WingsException ex) {
    List<ResponseMessage> responseMessages = ExceptionLogger.getResponseMessageList(ex, GRAPHQL_API);
    return responseMessages.stream().map(rm -> rm.getMessage()).collect(Collectors.joining(EXCEPTION_MSG_DELIMITER));
  }

  private F generateFilter(DataFetchingEnvironment environment, Class<F> filterClass, String key, String value) {
    Map<String, Object> map = new HashMap<>();
    map.put("type", key);
    Map stringFilterMap = new LinkedHashMap();
    stringFilterMap.put("operator", QLStringOperator.EQUALS.name());
    stringFilterMap.put("values", Arrays.asList(utils.getFieldValue(environment.getSource(), value)));
    map.put("stringFilter", stringFilterMap);
    return convertToObject(map, filterClass);
  }

  public String getAccountId(DataFetchingEnvironment environment) {
    return utils.getAccountId(environment);
  }

  private QLPageQueryParameters extractPageQueryParameters(DataFetchingEnvironment dataFetchingEnvironment) {
    final Integer offset = dataFetchingEnvironment.getArgument(OFFSET);
    final Integer limit = dataFetchingEnvironment.getArgument(LIMIT);

    if (limit != null && limit < 0) {
      throw new InvalidRequestException(NEGATIVE_LIMIT_ARG_MSG);
    }
    if (offset != null && offset < 0) {
      throw new InvalidRequestException(NEGATIVE_OFFSET_ARG_MSG);
    }

    return QLPageQueryParameterImpl.builder()
        .limit(limit == null ? 50 : limit)
        .offset(offset == null ? 0 : offset)
        .selectionSet(dataFetchingEnvironment.getSelectionSet())
        .build();
  }

  private Object fetchObject(DataFetchingEnvironment dataFetchingEnvironment, String fieldName, Class klass) {
    Object object = dataFetchingEnvironment.getArguments().get(fieldName);
    if (object == null) {
      return null;
    }

    if (object instanceof Collection) {
      Collection returnCollection = Lists.newArrayList();
      Collection collection = (Collection) object;
      collection.forEach(item -> returnCollection.add(convertToObject(item, klass)));
      return returnCollection;
    }
    return convertToObject(object, klass);
  }

  private <M> M convertToObject(Object fromValue, Class<M> klass) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(fromValue, klass);
  }

  protected abstract O fetchConnection(
      List<F> filters, QLPageQueryParameters pageQueryParameters, List<S> sortCriteria);

  protected final O fetch(List<F> filters, QLPageQueryParameters pageQueryParameters, List<S> sortCriteria) {
    try {
      return fetchConnection(filters, pageQueryParameters, sortCriteria);
    } catch (WingsException ex) {
      if (ErrorCode.ACCESS_DENIED.equals(ex.getCode())) {
        logger.warn("User doesn't have access to resource or no entities exist in that app");
      }
      throw ex;
    }
  }

  protected abstract String getFilterFieldName(String filterType);

  @NotNull
  public Query populateFilters(
      WingsPersistence wingsPersistence, List<? extends QLFilterType> filters, Class entityClass) {
    Query query = populateAccountFilter(wingsPersistence, entityClass);

    if (isNotEmpty(filters)) {
      filters.forEach(filter -> {
        if (filter.getDataType().equals(QLDataType.STRING)) {
          QLStringFilter stringFilter = ((QLStringFilterType) filter).getStringFilter();

          if (stringFilter == null) {
            throw new WingsException("Filter value is null for type:" + filter.getFilterType());
          }
          utils.setStringFilter(query.field(getFilterFieldName(filter.getFilterType())), stringFilter);
        } else if (((QLFilterType) filter).getDataType().equals(QLDataType.NUMBER)) {
          QLNumberFilter numberFilter = ((QLNumberFilterType) filter).getNumberFilter();

          if (numberFilter == null) {
            throw new WingsException("Filter value is null for type:" + filter.getFilterType());
          }
          utils.setNumberFilter(query.field(getFilterFieldName(filter.getFilterType())), numberFilter);
        }
      });
    }
    return query;
  }

  @NotNull
  public Query populateAccountFilter(WingsPersistence wingsPersistence, Class entityClass) {
    Query query = wingsPersistence.createQuery(entityClass);
    final String accountId = getAccountId();
    if (accountId != null) {
      query.filter(SettingAttributeKeys.accountId, accountId);
      return query;
    }
    return query;
  }

  public String getAccountId() {
    return AccountThreadLocal.get();
  }
}
