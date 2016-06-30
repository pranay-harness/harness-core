import com.google.common.collect.Lists;

import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.sm.StateExecutionInstance;

import javax.inject.Inject;

/**
 *
 */

/**
 * @author Rishi
 *
 */
public class QueryTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  @Test
  public void shouldQuery() {
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class);
    query.field("appId").equal("123").field("executionUuid").equal("234");
    query.or(query.criteria("parentInstanceId").doesNotExist(),
        query.criteria("parentInstanceId").in(Lists.newArrayList("abc", "def")));

    wingsPersistence.executeGetListQuery(query);
  }
}
