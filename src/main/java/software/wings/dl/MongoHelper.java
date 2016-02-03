package software.wings.dl;

import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.service.impl.AppServiceImpl;

public class MongoHelper {
  private static Logger logger = LoggerFactory.getLogger(AppServiceImpl.class);

  public static <T> PageResponse<T> queryPageRequest(
      Datastore datastore, Class<T> cls, PageRequest<T> req, String fieldName, String fieldValue) {
    SearchFilter filter = new SearchFilter();
    filter.setFieldName(fieldName);
    filter.setFieldValue(fieldValue);
    filter.setOp(OP.EQ);
    req.getFilters().add(filter);

    return queryPageRequest(datastore, cls, req);
  }
  public static <T> PageResponse<T> queryPageRequest(Datastore datastore, Class<T> cls, PageRequest<T> req) {
    Query q = datastore.createQuery(cls);
    q = MongoHelper.applyPageRequest(q, req);

    long total = q.countAll();
    q.offset(req.getStart());
    q.limit(req.getPageSize());
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
    OP op = filter.getOp();
    if (op == null) {
      op = OP.CONTAINS;
    }
    switch (op) {
      case LT:
        return fieldEnd.lessThan(filter.getFieldValue());

      case GT:
        return fieldEnd.greaterThan(filter.getFieldValue());

      case EQ:
        return fieldEnd.equal(filter.getFieldValue());

      case CONTAINS:
        return fieldEnd.containsIgnoreCase(filter.getFieldValue());

      case STARTSWITH:
        return fieldEnd.startsWithIgnoreCase(filter.getFieldValue());
    }
    return null;
  }
}
