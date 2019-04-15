package software.wings.dl;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.limits.LimitsMorphiaClasses;
import io.harness.mongo.HObjectFactory;
import io.harness.reflection.CodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import software.wings.app.WingsApplication;
import software.wings.beans.ManagerMorphiaClasses;
import software.wings.integration.common.MongoDBTest.MongoEntity;
import software.wings.integration.dl.PageRequestTest.Dummy;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MorphiaClassesTest {
  @Test
  @Category(UnitTests.class)
  public void testSearchAndList() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    Set<Class> classes = new HashSet();
    classes.addAll(WingsApplication.morphiaClasses);
    classes.add(Dummy.class);
    classes.add(MongoEntity.class);

    boolean success = true;
    for (MappedClass cls : morphia.getMapper().getMappedClasses()) {
      if (!classes.contains(cls.getClazz())) {
        logger.error(cls.getClazz().toString());
        success = false;
      }
    }

    assertThat(success).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testLimitsModule() {
    CodeUtils.checkHarnessClassBelongToModule(
        CodeUtils.location(LimitsMorphiaClasses.class), LimitsMorphiaClasses.classes);
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerModule() {
    CodeUtils.checkHarnessClassBelongToModule(
        CodeUtils.location(ManagerMorphiaClasses.class), ManagerMorphiaClasses.classes);
  }
}