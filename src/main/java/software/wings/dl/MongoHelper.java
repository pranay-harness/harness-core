package software.wings.dl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.apache.commons.lang3.ArrayUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.DatastoreImpl;
import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.ErrorCodes;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.exception.WingsException;

import java.util.Arrays;
import java.util.List;

// TODO: Auto-generated Javadoc

/**
 * The Class MongoHelper.
 */
public class MongoHelper {
  /**
   * Query page request.
   *
   * @param <T>       the generic type
   * @param datastore the datastore
   * @param cls       the cls
   * @param req       the req
   * @return the page response
   */
  public static <T> PageResponse<T> queryPageRequest(Datastore datastore, Class<T> cls, PageRequest<T> req) {
    Query q = datastore.createQuery(cls);

    Mapper mapper = ((DatastoreImpl) datastore).getMapper();
    MappedClass mappedClass = mapper.addMappedClass(cls);
    q = MongoHelper.applyPageRequest(q, req, mappedClass, mapper);

    long total = q.countAll();
    q.offset(req.getStart());
    if (PageRequest.UNLIMITED.equals(req.getLimit())) {
      q.limit(PageRequest.DEFAULT_UNLIMITED);
    } else {
      q.limit(req.getPageSize());
    }
    List<T> list = q.asList();

    PageResponse<T> response = new PageResponse<>(req);
    response.setTotal(total);
    response.setResponse(list);

    return response;
  }

  /**
   * Apply page request.
   *
   * @param <T>         the generic type
   * @param query       the query
   * @param req         the req
   * @param mappedClass the mapped class
   * @param mapper      the mapper
   * @return the query
   */
  public static <T> Query<T> applyPageRequest(
      Query<T> query, PageRequest<T> req, MappedClass mappedClass, Mapper mapper) {
    if (req == null) {
      return query;
    }

    req.populateFilters(mappedClass, mapper);

    if (req.getFilters() != null) {
      for (SearchFilter filter : req.getFilters()) {
        FieldEnd<? extends Query<T>> fieldEnd = query.field(filter.getFieldName());

        query = applyOperator(fieldEnd, filter);
      }
    }

    if (req.getOrders() != null) {
      for (SortOrder o : req.getOrders()) {
        if (o.getOrderType() == OrderType.DESC) {
          query.order("-" + o.getFieldName());
        } else {
          query.order(o.getFieldName());
        }
      }
    }

    return query;
  }

  private static <T> Query<T> applyOperator(FieldEnd<? extends Query<T>> fieldEnd, SearchFilter filter) {
    if (ArrayUtils.isEmpty(filter.getFieldValues())) {
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "message", "Unspecified fieldValue for search");
    }
    Operator op = filter.getOp();
    if (op == null) {
      op = Operator.EQ;
    }
    switch (op) {
      case LT:
        return fieldEnd.lessThan(filter.getFieldValues()[0]);

      case GT:
        return fieldEnd.greaterThan(filter.getFieldValues()[0]);

      case EQ:
        return fieldEnd.equal(filter.getFieldValues()[0]);

      case CONTAINS:
        return fieldEnd.containsIgnoreCase(String.valueOf(filter.getFieldValues()[0]));

      case STARTS_WITH:
        return fieldEnd.startsWithIgnoreCase(String.valueOf(filter.getFieldValues()[0]));

      case IN:
        return fieldEnd.hasAnyOf(Arrays.asList(filter.getFieldValues()));

      case NOT_IN:
        return fieldEnd.hasNoneOf(Arrays.asList(filter.getFieldValues()));
    }
    return null;
  }

  /**
   * Sets the unset.
   *
   * @param <T>   the generic type
   * @param ops   the ops
   * @param field the field
   * @param value the value
   * @return the update operations
   */
  public static <T> UpdateOperations<T> setUnset(UpdateOperations<T> ops, String field, Object value) {
    if (value == null || (value instanceof String && isBlank((String) value))) {
      return ops.unset(field);
    } else {
      return ops.set(field, value);
    }
  }
}
