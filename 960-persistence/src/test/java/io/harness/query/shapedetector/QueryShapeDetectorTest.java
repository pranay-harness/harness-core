package io.harness.query.shapedetector;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import lombok.Value;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryShapeDetectorTest extends CategoryTest {
  private static final List<List<CalculateHashParams>> sameShapeQueriesList = Arrays.asList(
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
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_oid").is(new ObjectId("abc")))),
          createCalculateHashParams(Query.query(Criteria.where("_oid").is(new ObjectId("def"))))),
      Arrays.asList(createCalculateHashParams(Query.query(Criteria.where("_array").is(Arrays.asList(1, 2)))),
          createCalculateHashParams(Query.query(Criteria.where("_array").is(Arrays.asList(3, 4, 5))))));

  @Test
  @Owner(developers = GARVIT)
  @Category(io.harness.category.element.UnitTests.class)
  public void testCalculateHash() {
    for (List<CalculateHashParams> sameShapeQueries : sameShapeQueriesList) {
      String hash = QueryShapeDetector.calculateQueryHash(
          sameShapeQueries.get(0).getCollectionName(), sameShapeQueries.get(0).getQueryDoc());
      for (int i = 1; i < sameShapeQueries.size(); i++) {
        CalculateHashParams params = sameShapeQueries.get(i);
        assertThat(QueryShapeDetector.calculateQueryHash(params.getCollectionName(), params.getQueryDoc()))
            .isEqualTo(hash);
      }
    }
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testMongoQueryHash() {
    int hash1 = 0;
    int hash2 = 0;
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      String jsonString = "{planExecutionId: \"pid\", status : {$in : [\"RUNNING\", \"WAITING\"]} }";
      JsonNode jsonNode = objectMapper.readTree(objectMapper.getFactory().createParser(jsonString));
      hash1 = jsonNode.hashCode();
      jsonString = "{status : {$in : [\"RUNNING\", \"WAITING\"]}, planExecutionId: \"pid\" }";
      jsonNode = objectMapper.readTree(objectMapper.getFactory().createParser(jsonString));
      hash2 = jsonNode.hashCode();
    } catch (IOException e) {
      throw new InvalidRequestException("Unable to read the json ", e);
    }
    assertThat(hash1).isEqualTo(hash2);
  }

  @Value
  private static class CalculateHashParams {
    String collectionName;
    Document queryDoc;
  }

  private static CalculateHashParams createCalculateHashParams(Query query) {
    return new CalculateHashParams("randomColl", query.getQueryObject());
  }
}
