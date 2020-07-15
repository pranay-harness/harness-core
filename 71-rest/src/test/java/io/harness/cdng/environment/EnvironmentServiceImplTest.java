package io.harness.cdng.environment;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.Tag;
import io.harness.cdng.environment.beans.Environment;
import io.harness.cdng.environment.beans.Environment.EnvironmentKeys;
import io.harness.cdng.environment.beans.EnvironmentType;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.Collections;

public class EnvironmentServiceImplTest extends WingsBaseTest {
  @Inject private HPersistence hPersistence;
  @Inject private EnvironmentServiceImpl environmentService;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testSave() {
    Environment environment = Environment.builder()
                                  .identifier("test-id")
                                  .environmentType(EnvironmentType.PreProduction)
                                  .tags(Collections.singletonList(Tag.builder().key("key1").value("value1").build()))
                                  .build();
    environmentService.save(environment);

    Environment savedEnvironment =
        hPersistence.createQuery(Environment.class).filter(EnvironmentKeys.identifier, "test-id").get();
    assertThat(savedEnvironment).isNotNull();

    testSaveInvalidScenarios(environment);
  }

  private void testSaveInvalidScenarios(Environment environment) {
    // empty identifier
    environment.setIdentifier(null);
    assertThatThrownBy(() -> environmentService.save(environment))
        .hasMessageContaining("Environment identifier can't be empty");
    environment.setIdentifier("test-id");

    // empty type
    environment.setEnvironmentType(null);
    assertThatThrownBy(() -> environmentService.save(environment))
        .hasMessageContaining("Environment Type can't be empty");
    environment.setEnvironmentType(EnvironmentType.PreProduction);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testUpdate() {
    // first save
    Environment environment = Environment.builder()
                                  .identifier("test-id")
                                  .environmentType(EnvironmentType.PreProduction)
                                  .tags(Collections.singletonList(Tag.builder().key("key1").value("value1").build()))
                                  .build();
    environmentService.save(environment);

    Environment savedEnvironment =
        hPersistence.createQuery(Environment.class).filter(EnvironmentKeys.identifier, "test-id").get();
    assertThat(savedEnvironment).isNotNull();

    // test update
    environment.setDisplayName("new Display Name");
    environment.setTags(Collections.singletonList(Tag.builder().key("key2").value("value2").build()));
    environmentService.update(savedEnvironment, environment);
    savedEnvironment = hPersistence.createQuery(Environment.class).filter(EnvironmentKeys.identifier, "test-id").get();
    assertThat(savedEnvironment.getDisplayName()).isEqualTo("new Display Name");
    assertThat(savedEnvironment.getTags()).hasSize(2);

    // immutable fields
    environment.setEnvironmentType(EnvironmentType.Production);
    Environment finalSavedEnvironment = savedEnvironment;
    assertThatThrownBy(() -> environmentService.update(finalSavedEnvironment, environment))
        .hasMessageContaining("Environment type is immutable. Existing: [PreProduction], New: [Production]");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testUpsert() {
    // save
    Environment environment =
        Environment.builder().identifier("test-id").environmentType(EnvironmentType.PreProduction).build();
    environmentService.upsert(environment);

    // get
    Environment savedEnvironment =
        hPersistence.createQuery(Environment.class).filter(EnvironmentKeys.identifier, "test-id").get();
    assertThat(savedEnvironment).isNotNull();

    // update
    environment.setDisplayName("new Display Name");
    environmentService.upsert(environment);
    savedEnvironment = hPersistence.createQuery(Environment.class).filter(EnvironmentKeys.identifier, "test-id").get();
    assertThat(savedEnvironment.getDisplayName()).isEqualTo("new Display Name");
  }
}