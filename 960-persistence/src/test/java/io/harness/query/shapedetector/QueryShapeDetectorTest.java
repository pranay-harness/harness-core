package io.harness.query.shapedetector;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import lombok.Value;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryShapeDetectorTest extends CategoryTest {
  // TODO:
  // - add test cases for null and define behaviour

  private static final List<List<CalculateHashParams>> sameShapeQueriesList = Arrays.asList(
      // Basic types
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_str").is("abc"))),
          createCalculateHashParams(Query.query(Criteria.where("_str").is("def")))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_int").is(1))),
          createCalculateHashParams(Query.query(Criteria.where("_int").is(10)))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_long").is(1L))),
          createCalculateHashParams(Query.query(Criteria.where("_long").is(10L)))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_double").is(56.98))),
          createCalculateHashParams(Query.query(Criteria.where("_double").is(25.0)))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_decimal").is(new BigDecimal("123.456")))),
          createCalculateHashParams(Query.query(Criteria.where("_decimal").is(new BigDecimal("456.789"))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_bool").is(true))),
          createCalculateHashParams(Query.query(Criteria.where("_bool").is(false)))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_oid").is(new ObjectId(new Date())))),
          createCalculateHashParams(
              Query.query(Criteria.where("_oid").is(new ObjectId(new Date(System.currentTimeMillis() + 5000)))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_array").is(Arrays.asList(1, 2)))),
          createCalculateHashParams(Query.query(Criteria.where("_array").is(Arrays.asList(3, 4, 5))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_binary").is("abc".getBytes()))),
          createCalculateHashParams(Query.query(Criteria.where("_binary").is("def".getBytes())))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_date").is(new Date()))),
          createCalculateHashParams(
              Query.query(Criteria.where("_date").is(new Date(System.currentTimeMillis() + 5000))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_ts").is(new BsonTimestamp(50)))),
          createCalculateHashParams(Query.query(Criteria.where("_ts").is(new BsonTimestamp(1000))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_regex").is(Pattern.compile("abc")))),
          createCalculateHashParams(Query.query(Criteria.where("_regex").is(Pattern.compile("def"))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_obj").is(ImmutablePair.of(1, "abc")))),
          createCalculateHashParams(Query.query(Criteria.where("_obj").is(ImmutablePair.of("def", 5))))),

      // Order of params
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").is("b"))),
          createCalculateHashParams(Query.query(Criteria.where("_b").is("a").and("_a").is("b")))),
      Arrays.asList(
          createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").in(Arrays.asList(1, 2, 3)))),
          createCalculateHashParams(Query.query(Criteria.where("_b").in(Arrays.asList(4, 5)).and("_a").is("b")))),

      // in operator
      Arrays.asList(
          createCalculateHashParams(Query.query(
              Criteria.where("planExecutionId").is("p1").and("status").in(Arrays.asList("RUNNING", "WAITING")))),
          createCalculateHashParams(Query.query(
              Criteria.where("status").in(Arrays.asList("WAITING", "RUNNING")).and("planExecutionId").is("p3")))),

      // Operators
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b._c").maxDistance(10))),
          createCalculateHashParams(Query.query(Criteria.where("_b._c").maxDistance(15).and("_a").is("b")))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").lte(10))),
          createCalculateHashParams(Query.query(Criteria.where("_b").lte(15).and("_a").is("b")))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").exists(true))),
          createCalculateHashParams(Query.query(Criteria.where("_b").exists(false).and("_a").is("b")))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").elemMatch(
                        Criteria.where("_c").is("c").and("_d").is("d")))),
          createCalculateHashParams(Query.query(
              Criteria.where("_b").elemMatch(Criteria.where("_d").is("c").and("_c").is("d")).and("_a").is("b")))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").size(5))),
          createCalculateHashParams(Query.query(Criteria.where("_b").size(8).and("_a").is("b")))),
      Arrays.asList(
          createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").near(new Point(1.0, 2.0)))),
          createCalculateHashParams(Query.query(Criteria.where("_b").near(new Point(2.0, 3.0)).and("_a").is("b")))),

      // Logical operators
      Arrays.asList(
          createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").exists(true).orOperator(
              Criteria.where("_c").is("c").and("_d").is("d")))),
          createCalculateHashParams(
              Query.query(Criteria.where("_b")
                              .exists(false)
                              .orOperator(Criteria.where("_d").is("c").and("_c").is("d"))
                              .and("_a")
                              .is("b")))),
      Arrays.asList(
          createCalculateHashParams(Query.query(Criteria.where("_a").is("a").and("_b").exists(true).norOperator(
              Criteria.where("_c").is("c").and("_d").is("d")))),
          createCalculateHashParams(
              Query.query(Criteria.where("_b")
                              .exists(false)
                              .norOperator(Criteria.where("_d").is("c").and("_c").is("d"))
                              .and("_a")
                              .is("b")))));

  private static final List<Pair<CalculateHashParams, CalculateHashParams>> diffShapeQueriesList = Arrays.asList(
      // Basic types
      ImmutablePair.of(createCalculateHashParams(Query.query(Criteria.where("_str").is("abc"))),
          createCalculateHashParams(Query.query(Criteria.where("_def").is("def")))),
      ImmutablePair.of(createCalculateHashParams(Query.query(Criteria.where("planExecutionId").is("p1"))),
          createCalculateHashParams(Query.query(Criteria.where("status").is("s1")))),
      ImmutablePair.of(createCalculateHashParams(Query.query(Criteria.where("_str").is("abc"))),
          createCalculateHashParams(Query.query(Criteria.where("_str").is(null)))),
      ImmutablePair.of(createCalculateHashParams(Query.query(Criteria.where("_a").is(1))),
          createCalculateHashParams(Query.query(Criteria.where("_a").is(10).and("_b").is(5)))));

  @Test
  @Owner(developers = GARVIT)
  @Category(io.harness.category.element.UnitTests.class)
  public void testNormalizeMapSameShape() {
    for (List<CalculateHashParams> sameShapeQueries : sameShapeQueriesList) {
      String normalized = JsonUtils.asJson(QueryShapeDetector.normalizeObject(sameShapeQueries.get(0).getQueryDoc()));
      for (int i = 1; i < sameShapeQueries.size(); i++) {
        CalculateHashParams params = sameShapeQueries.get(i);
        assertThat(JsonUtils.asJson(QueryShapeDetector.normalizeObject(params.getQueryDoc()))).isEqualTo(normalized);
      }
    }
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(io.harness.category.element.UnitTests.class)
  public void testNormalizeMapDiffShape() {
    for (Pair<CalculateHashParams, CalculateHashParams> diffShapeQueries : diffShapeQueriesList) {
      String normalized =
          JsonUtils.asJson(QueryShapeDetector.normalizeObject(diffShapeQueries.getLeft().getQueryDoc()));
      assertThat(JsonUtils.asJson(QueryShapeDetector.normalizeObject(diffShapeQueries.getRight().getQueryDoc())))
          .isNotEqualTo(normalized);
    }
  }

  @Value
  private static class CalculateHashParams {
    String collectionName;
    Document queryDoc;
  }

  private static CalculateHashParams createCalculateHashParams(Query query) {
    new CalculateHashParams("randomColl", query.getQueryObject());
    return new CalculateHashParams("randomColl", query.getQueryObject());
  }
}
