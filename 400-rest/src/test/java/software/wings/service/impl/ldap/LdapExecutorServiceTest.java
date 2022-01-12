/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.ldap;

import static io.harness.rule.OwnerRule.VIKAS;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.impl.ldap.LdapExecutorService.LdapExecutorServiceKeys;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapExecutorServiceTest extends WingsBaseTest {
  @Before
  public void setUp() {}

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetInstance_forReturningSameInstance() {
    LdapExecutorService ldapExecutorService1 = LdapExecutorService.getInstance();
    LdapExecutorService ldapExecutorService2 = LdapExecutorService.getInstance();
    assertThat(ldapExecutorService1).isEqualTo(ldapExecutorService2);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetExecutorService_forLazyLoading() throws NoSuchFieldException, IllegalAccessException {
    LdapExecutorService ldapExecutorService = LdapExecutorService.getInstance();

    Field f = ldapExecutorService.getClass().getDeclaredField(LdapExecutorServiceKeys.executorService);
    f.setAccessible(true);
    AtomicReference atomicReference = (AtomicReference) f.get(ldapExecutorService);
    assertThat(atomicReference.get()).isNull();

    ExecutorService executorService = ldapExecutorService.getExecutorService();
    assertThat(executorService).isNotNull();
    assertThat(((ThreadPoolExecutor) executorService).getCorePoolSize())
        .isEqualTo(Runtime.getRuntime().availableProcessors());
    assertThat(((ThreadPoolExecutor) executorService).getMaximumPoolSize())
        .isEqualTo(2 * Runtime.getRuntime().availableProcessors());

    atomicReference = (AtomicReference) f.get(ldapExecutorService);
    assertThat(atomicReference.get()).isNotNull();
    assertThat(atomicReference.get()).isEqualTo(executorService);
  }
}
