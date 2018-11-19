package io.harness.dl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.app.VerificationServiceApplication;
import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;

public class MorphiaClassesTest {
  @Test
  public void testElemMatchPageRequest() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("wings.software");
    morphia.mapPackage("io.harness");

    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      assertThat(VerificationServiceApplication.morphiaClasses).contains(cls.getClazz());
    }
  }
}