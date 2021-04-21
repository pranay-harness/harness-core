package io.harness.mongo;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class MongoDotTest extends PipelineServiceTestBase {
  @Inject private MongoTemplate mongoTemplate;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void testMapWithDotKeySerialization() {
    String id = generateUuid();
    ImmutableMap<String, String> valMap = ImmutableMap.of("f.g", "h.i");
    MapDotTestPersistentEntity mapDotTestPersistentEntity =
        MapDotTestPersistentEntity.builder().id(id).keyValPair("a.b", "c").keyValPair("d.e", valMap).build();
    mongoTemplate.insert(mapDotTestPersistentEntity);
    Query query = query(where("id").is(id));
    MapDotTestPersistentEntity persistentEntity = mongoTemplate.findOne(query, MapDotTestPersistentEntity.class);
    assertThat(persistentEntity).isNotNull();
    assertThat(persistentEntity.keyValPairs.keySet()).containsExactly("a.b", "d.e");
    assertThat(persistentEntity.keyValPairs.values()).containsExactly("c", valMap);
  }
}
