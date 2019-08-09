package software.wings.dl;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.category.element.UnitTests;
import io.harness.mongo.HObjectFactory;
import io.harness.reflection.CodeUtils;
import io.harness.serializer.morphia.LimitsMorphiaRegistrar;
import io.harness.serializer.morphia.ManagerMorphiaRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.MappedClass;
import software.wings.WingsBaseTest;
import software.wings.integration.common.MongoDBTest.MongoEntity;
import software.wings.integration.dl.PageRequestTest.Dummy;

import java.util.HashSet;
import java.util.Set;

@Slf4j
public class RestMorphiaClassesTest extends WingsBaseTest {
  @Inject @Named("morphiaClasses") Set<Class> morphiaClasses;

  @Test
  @Category(UnitTests.class)
  public void testSearchAndList() {
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");

    final HashSet<Class> classes = new HashSet<>(morphiaClasses);
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
    final HashSet<Class> classes = new HashSet<>();
    new LimitsMorphiaRegistrar().registerClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(LimitsMorphiaRegistrar.class), classes);
  }

  @Test
  @Category(UnitTests.class)
  public void testManagerModule() {
    final HashSet<Class> classes = new HashSet<>();
    new ManagerMorphiaRegistrar().registerClasses(classes);
    CodeUtils.checkHarnessClassBelongToModule(CodeUtils.location(ManagerMorphiaRegistrar.class), classes);
  }
}