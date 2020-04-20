package io.harness.entities;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.IndexManager;
import io.harness.mongo.IndexManagerSession;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import software.wings.beans.Account;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class MongoIndexesTest extends VerificationBaseTest {
  @Inject HPersistence persistence;
  @Inject HObjectFactory objectFactory;

  Set<Class> classes = VerificationMorphiaClasses.classes;

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testConfirmAllIndexes() throws IOException {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(objectFactory);
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.map(classes);

    List<IndexManager.IndexCreator> indexCreators =
        IndexManagerSession.allIndexes(persistence.getDatastore(Account.class), morphia);

    List<String> indexes = indexCreators.stream()
                               .map(creator
                                   -> creator.getCollection().getName() + " " + creator.getOptions().toString() + " "
                                       + creator.getKeys().toString())
                               .sorted()
                               .collect(Collectors.toList());

    List<String> expectedIndexes;
    try (InputStream in = getClass().getResourceAsStream("/mongo/indexes.txt")) {
      expectedIndexes = IOUtils.readLines(in, "UTF-8");
    }

    assertThat(indexes).isEqualTo(expectedIndexes);
  }
}
