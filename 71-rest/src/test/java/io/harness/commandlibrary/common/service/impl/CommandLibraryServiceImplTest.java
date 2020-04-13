package io.harness.commandlibrary.common.service.impl;

import static io.harness.commandlibrary.common.CommandLibraryConstants.MANAGER_CLIENT_ID;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.commandlibrary.common.service.CommandLibraryService;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceSecretKey;
import software.wings.dl.WingsPersistence;

public class CommandLibraryServiceImplTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;

  CommandLibraryService commandLibraryService;

  @Before
  public void setUp() throws Exception {
    commandLibraryService = new CommandLibraryServiceImpl(wingsPersistence);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getSecretForClient() {
    final ServiceSecretKey serviceSecretKey =
        ServiceSecretKey.builder()
            .serviceSecret("secret")
            .serviceType(ServiceSecretKey.ServiceType.MANAGER_TO_COMMAND_LIBRARY_SERVICE)
            .build();

    wingsPersistence.save(serviceSecretKey);
    final String secretForClient = commandLibraryService.getSecretForClient(MANAGER_CLIENT_ID);
    Assertions.assertThat(secretForClient).isEqualTo("secret");
  }
}