package io.harness.resources;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.resources.DelegateUpgraderResource;
import io.harness.service.intfc.DelegateUpgraderService;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(DEL)
@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class DelegateUpgraderResourceTest extends JerseyTest {
  private static final String ACCOUNT_ID = "account_id";
  private static final String IMAGE_TAG = "harness/delegate:1";

  @Mock private DelegateUpgraderService upgraderService;

  @Override
  protected Application configure() {
    // Mocks have to be initiated because hey, why shouldn't JerseyTest have a bad design
    initMocks(this);
    final ResourceConfig resourceConfig = new ResourceConfig();
    resourceConfig.register(new DelegateUpgraderResource(upgraderService));
    return resourceConfig;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
    return new InMemoryTestContainerFactory();
  }

  @Before
  public void cantBeCalledSetUpBecauseJerseyTestIsEvil() {
    // Nothing to do for now
  }

  @Test
  public void testSomething() {
    when(upgraderService.getDelegateImageTag(ACCOUNT_ID, IMAGE_TAG)).thenReturn(Pair.of(false, IMAGE_TAG));

    final Response response =
        client()
            .target("/upgrader/delegate?accountId=" + ACCOUNT_ID + "&currentDelegateImageTag=" + IMAGE_TAG)
            .request()
            .get();

    // use json-path to assert anything in json
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
