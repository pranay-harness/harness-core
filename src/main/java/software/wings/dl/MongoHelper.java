package software.wings.dl;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;

import java.util.List;

public class MongoHelper {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static <T> PageResponse<T> queryPageRequest(
      Datastore datastore, Class<T> cls, PageRequest<T> req, String fieldName, String fieldValue) {
    if (req == null) {
      req = new PageRequest<>();
    }
    SearchFilter filter = new SearchFilter();
    filter.setFieldName(fieldName);
    filter.setFieldValue(fieldValue);
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);

    return queryPageRequest(datastore, cls, req);
  }

  public static <T> PageResponse<T> queryPageRequest(Datastore datastore, Class<T> cls, PageRequest<T> req) {
    Query q = datastore.createQuery(cls);
    q = MongoHelper.applyPageRequest(q, req);

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

  public static <T> Query<T> applyPageRequest(Query<T> query, PageRequest<T> req) {
    if (req == null) {
      return query;
    }

    req.populateFilters();

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
    Operator op = filter.getOp();
    if (op == null) {
      op = Operator.EQ;
    }
    switch (op) {
      case LT:
        return fieldEnd.lessThan(filter.getFieldValue());

      case GT:
        return fieldEnd.greaterThan(filter.getFieldValue());

      case EQ:
        return fieldEnd.equal(filter.getFieldValue());

      case CONTAINS:
        return fieldEnd.containsIgnoreCase(String.valueOf(filter.getFieldValue()));

      case STARTS_WITH:
        return fieldEnd.startsWithIgnoreCase(String.valueOf(filter.getFieldValue()));

      case IN:
        return fieldEnd.hasAnyOf(filter.getFieldValues());

      case NOT_IN:
        return fieldEnd.hasNoneOf(filter.getFieldValues());
    }
    return null;
  }
}
