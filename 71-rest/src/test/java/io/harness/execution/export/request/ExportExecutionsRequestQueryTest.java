package io.harness.execution.export.request;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.mongodb.DBObject;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;

public class ExportExecutionsRequestQueryTest extends WingsBaseTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @SuppressWarnings("deprecation")
  public void testConversion() {
    assertThat(ExportExecutionsRequestQuery.fromQuery(null)).isNull();

    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class).filter("accountId", "aid");
    ExportExecutionsRequestQuery requestQuery = ExportExecutionsRequestQuery.fromQuery(query);
    assertThat(requestQuery).isNotNull();
    assertThat(requestQuery.getDbObjectJson()).isNotNull();

    Query<WorkflowExecution> newQuery = wingsPersistence.createQuery(WorkflowExecution.class);
    assertThatCode(() -> ExportExecutionsRequestQuery.updateQuery(null, requestQuery)).doesNotThrowAnyException();
    assertThatCode(() -> ExportExecutionsRequestQuery.updateQuery(query, null)).doesNotThrowAnyException();

    ExportExecutionsRequestQuery.updateQuery(newQuery, requestQuery);
    DBObject dbObject = newQuery.getQueryObject();
    assertThat(dbObject).isNotNull();
  }
}
