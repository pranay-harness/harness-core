package io.harness.dl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DataGenApplication;
import io.harness.OrchestrationMorphiaClasses;
import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class MorphiaClassesTest {
  private static final Logger logger = LoggerFactory.getLogger(MorphiaClassesTest.class);

  @Test
  @Category(UnitTests.class)
  public void testSearchAndList() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    Set<Class> classes = new HashSet();
    classes.addAll(DataGenApplication.morphiaClasses);

    classes.addAll(OrchestrationMorphiaClasses.classes);

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!classes.contains(cls.getClazz())) {
        logger.error(cls.getClazz().toString());
        success = false;
      }
    }

    assertThat(success).isTrue();
  }
}