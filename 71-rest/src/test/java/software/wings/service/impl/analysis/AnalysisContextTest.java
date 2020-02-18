package software.wings.service.impl.analysis;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.utils.Misc.replaceDotWithUnicode;

import com.google.common.collect.Sets;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AnalysisContextTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testIteration() throws IllegalAccessException {
    final AnalysisContext analysisContext = AnalysisContext.builder().stateExecutionId(generateUuid()).build();
    long timeSeriesAnalysisIteration = random.nextLong();
    FieldUtils.writeField(
        analysisContext, AnalysisContextKeys.timeSeriesAnalysisIteration, timeSeriesAnalysisIteration, true);
    long logAnalysisIteration = random.nextLong();
    FieldUtils.writeField(analysisContext, AnalysisContextKeys.logAnalysisIteration, logAnalysisIteration, true);

    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration))
        .isEqualTo(timeSeriesAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.logAnalysisIteration))
        .isEqualTo(logAnalysisIteration);

    timeSeriesAnalysisIteration = random.nextLong();
    analysisContext.updateNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration, timeSeriesAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.timeSeriesAnalysisIteration))
        .isEqualTo(timeSeriesAnalysisIteration);

    logAnalysisIteration = random.nextLong();
    analysisContext.updateNextIteration(AnalysisContextKeys.logAnalysisIteration, logAnalysisIteration);
    assertThat(analysisContext.obtainNextIteration(AnalysisContextKeys.logAnalysisIteration))
        .isEqualTo(logAnalysisIteration);

    try {
      analysisContext.updateNextIteration(generateUuid(), random.nextLong());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }

    try {
      analysisContext.obtainNextIteration(generateUuid());
      fail("Did not throw exception");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHostsWithDots() {
    Set<String> controlNodes = Sets.newHashSet("harness.todolist.control1", "harness.todolist.control2");
    Set<String> testNodes = Sets.newHashSet("harness.todolist.test1", "harness.todolist.test2");

    final AnalysisContext analysisContext = AnalysisContext.builder()
                                                .stateExecutionId(generateUuid())
                                                .controlNodes(nodesWithGroups(controlNodes))
                                                .testNodes(nodesWithGroups(testNodes))
                                                .build();
    final String analysisContextId = wingsPersistence.save(analysisContext);
    final DBCollection collection = wingsPersistence.getCollection(DEFAULT_STORE, "verificationServiceTask");
    final BasicDBObject dbObject = (BasicDBObject) collection.findOne();
    dbObject.get(AnalysisContextKeys.controlNodes);
    verifyDotsReplacedWithUniCode((BasicDBObject) dbObject.get(AnalysisContextKeys.controlNodes), controlNodes);
    verifyDotsReplacedWithUniCode((BasicDBObject) dbObject.get(AnalysisContextKeys.testNodes), testNodes);

    final AnalysisContext savedAnalysisContext = wingsPersistence.get(AnalysisContext.class, analysisContextId);
    assertThat(savedAnalysisContext.getControlNodes().keySet()).isEqualTo(controlNodes);
    assertThat(savedAnalysisContext.getTestNodes().keySet()).isEqualTo(testNodes);
  }

  private void verifyDotsReplacedWithUniCode(BasicDBObject nodesInDb, Set<String> nodesToVerify) {
    Set<String> unicodeNodes = new HashSet<>();
    nodesToVerify.forEach(node -> unicodeNodes.add(replaceDotWithUnicode(node)));
    assertThat(nodesInDb.keySet().size()).isEqualTo(nodesToVerify.size());
    nodesInDb.keySet().forEach(node -> {
      assertThat(nodesToVerify).doesNotContain(node);
      assertThat(unicodeNodes).contains(node);
    });
  }

  private Map<String, String> nodesWithGroups(Set<String> nodes) {
    Map<String, String> rv = new HashMap<>();
    nodes.forEach(node -> rv.put(node, DEFAULT_GROUP_NAME));
    return rv;
  }
}
