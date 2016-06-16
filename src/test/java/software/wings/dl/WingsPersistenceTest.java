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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.inject.Inject;
import javax.ws.rs.core.AbstractMultivaluedMap;
import javax.ws.rs.core.UriInfo;

// TODO: Auto-generated Javadoc
/**
 * The Class WingsPersistenceTest.
 */

/**
 * The type Wings persistence test.
 *
 * @author Rishi
 */
public class WingsPersistenceTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  /**
   * Should query by in operator.
   */
  @Test
  public void shouldSaveList() {
    List<TestEntity> list = Lists.newArrayList();
    IntStream.range(0, 5).forEach(i -> {
      TestEntity entity = new TestEntity();
      entity.setFieldA("fieldA" + i);
      list.add(entity);
    });
    List<String> ids = wingsPersistence.save(list);

    assertThat(ids).isNotNull().hasSize(list.size()).doesNotContainNull();
  }

  /**
   * Should query by in operator.
   */
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

    filter.setFieldValues("fieldA11", "fieldA21");
    filter.setFieldName("fieldA");
    filter.setOp(Operator.IN);
    req.addFilter(filter);
    PageResponse<TestEntity> res = wingsPersistence.query(TestEntity.class, req);
    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);
  }

  /**
   * Should paginate filter sort.
   */
  @Test
  public void shouldPaginateFilterSort() {
    createEntitiesForPagination();

    PageRequest<TestEntity> req = new PageRequest<>();
    req.setLimit("2");
    req.setOffset("1");
    SearchFilter filter = new SearchFilter();
    filter.setFieldValues("fieldA1");
    filter.setFieldName("fieldA");
    filter.setOp(Operator.CONTAINS);
    req.addFilter(filter);

    SortOrder order = new SortOrder();
    order.setFieldName("fieldA");
    order.setOrderType(OrderType.DESC);
    req.addOrder(order);

    PageResponse<TestEntity> res = wingsPersistence.query(TestEntity.class, req);

    assertPaginationResult(res);
  }

  /**
   * Should take query params.
   */
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

  /**
   * Should update map
   */
  @Test
  public void shouldUpdateMap() {
    TestEntity entity = new TestEntity();
    entity.setFieldA("fieldA11");
    Map<String, String> map = new HashMap<>();
    map.put("abc", "123");
    entity.setMapField(map);
    wingsPersistence.save(entity);

    TestEntity entity1 = wingsPersistence.get(TestEntity.class, entity.getUuid());

    Map<String, String> map2 = new HashMap<>();
    map2.put("abcd", "1234");

    Map fieldMap = new HashMap<>();
    fieldMap.put("mapField", map2);
    wingsPersistence.updateFields(TestEntity.class, entity.getUuid(), fieldMap);

    TestEntity entity2 = wingsPersistence.get(TestEntity.class, entity.getUuid());
    assertThat(entity2).isNotNull();
    assertThat(entity2.getMapField()).isEqualTo(map2);
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

  /**
   * The Class TestEntity.
   */
  public static class TestEntity extends Base {
    private String fieldA;
    private Map<String, String> mapField;

    /**
     * Gets field a.
     *
     * @return the field a
     */
    public String getFieldA() {
      return fieldA;
    }

    /**
     * Sets field a.
     *
     * @param fieldA the field a
     */
    public void setFieldA(String fieldA) {
      this.fieldA = fieldA;
    }

    public Map<String, String> getMapField() {
      return mapField;
    }

    public void setMapField(Map<String, String> mapField) {
      this.mapField = mapField;
    }
  }
}
