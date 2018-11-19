package software.wings.dl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.mongo.NoDefaultConstructorMorphiaObjectFactory;
import org.junit.Test;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import software.wings.app.WingsApplication;

public class MorphiaClassesTest {
  @Test
  public void testElemMatchPageRequest() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new NoDefaultConstructorMorphiaObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("wings.software");
    morphia.mapPackage("io.harness");

    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      assertThat(WingsApplication.morphiaClasses).contains(cls.getClazz());
    }
  }
}