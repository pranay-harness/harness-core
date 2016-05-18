package software.wings.dl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 *
 */

/**
 * @author Rishi
 *
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
