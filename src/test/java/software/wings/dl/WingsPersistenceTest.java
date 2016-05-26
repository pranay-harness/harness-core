package software.wings.dl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.util.Lists;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 *
 */

/**
 * @author Rishi
 */
public class WingsPersistenceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldQueryByINOperator() {
    TestEntity entity = new TestEntity();
    entity.setFieldA("fieldA11");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA12");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA21");
    wingsPersistence.save(entity);

    PageRequest<TestEntity> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    List<Object> fieldValues = new ArrayList<>();
    fieldValues.add("fieldA11");
    fieldValues.add("fieldA21");
    filter.setFieldValues(fieldValues);
    filter.setFieldName("fieldA");
    filter.setOp(Operator.IN);
    req.getFilters().add(filter);
    PageResponse<TestEntity> res = wingsPersistence.query(TestEntity.class, req);
    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);
  }

  @Test
  public void shouldPaginateFilterSort() {
    createEntitiesForPagination();

    PageRequest<TestEntity> req = new PageRequest<>();
    req.setLimit("2");
    req.setOffset("1");
    SearchFilter filter = new SearchFilter();
    filter.setFieldValue("fieldA1");
    filter.setFieldName("fieldA");
    filter.setOp(Operator.CONTAINS);
    req.getFilters().add(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("fieldA");
    order.setOrderType(OrderType.DESC);
    req.getOrders().add(order);

    PageResponse<TestEntity> res = wingsPersistence.query(TestEntity.class, req);

    assertPaginationResult(res);
  }

  @Test
  public void shouldTakeQueryParams() {
    UriInfo uriInfo = mock(UriInfo.class);
    Map<String, List<String>> queryParams = new HashMap<>();
    queryParams.put("search[0][field]", Lists.newArrayList("fieldA"));
    queryParams.put("search[0][op]", Lists.newArrayList("CONTAINS"));
    queryParams.put("search[0][value]", Lists.newArrayList("fieldA1"));

    queryParams.put("sort[0][field]", Lists.newArrayList("fieldA"));
    queryParams.put("sort[0][direction]", Lists.newArrayList("DESC"));

    when(uriInfo.getQueryParameters()).thenReturn(new AbstractMultivaluedMap<String, String>(queryParams) {});
    createEntitiesForPagination();

    PageRequest<TestEntity> req = new PageRequest<>();
    req.setLimit("2");
    req.setOffset("1");
    req.setUriInfo(uriInfo);

    PageResponse<TestEntity> res = wingsPersistence.query(TestEntity.class, req);

    assertPaginationResult(res);
  }

  private void assertPaginationResult(PageResponse<TestEntity> res) {
    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);
    assertThat(res.getTotal()).isEqualTo(5);
    assertThat(res.getStart()).isEqualTo(1);
    assertThat(res.get(0)).isNotNull();
    assertThat(res.get(0).getFieldA()).isEqualTo("fieldA14");
    assertThat(res.get(1)).isNotNull();
    assertThat(res.get(1).getFieldA()).isEqualTo("fieldA13");
  }

  private void createEntitiesForPagination() {
    TestEntity entity = new TestEntity();
    entity.setFieldA("fieldA11");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA12");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA13");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA14");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA15");
    wingsPersistence.save(entity);

    entity = new TestEntity();
    entity.setFieldA("fieldA21");
    wingsPersistence.save(entity);
  }

  public static class TestEntity extends Base {
    private String fieldA;

    public String getFieldA() {
      return fieldA;
    }

    public void setFieldA(String fieldA) {
      this.fieldA = fieldA;
    }
  }
}
