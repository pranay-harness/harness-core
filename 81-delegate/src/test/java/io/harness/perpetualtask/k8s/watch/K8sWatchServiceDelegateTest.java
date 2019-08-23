package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.perpetualtask.k8s.watch.K8SWatch.K8sWatchTaskParams;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
@PrepareForTest(WatchService.class)
@Slf4j
public class K8sWatchServiceDelegateTest {
  private String id;
  final CountDownLatch closeLatch = new CountDownLatch(1);

  @Inject private static K8sWatchServiceDelegate watchServiceDelegate;

  @BeforeClass
  public static void setup() {
    // Injector injector = Guice.createInjector(new KubernetesClientFactoryModule());
    // watchService = injector.getInstance(K8sWatchService.class);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a valid local kubeconfig file.")
  public void testCreate() throws Exception {
    logger.info("Test registering a WatchRequest in a WatchService..");
    K8sWatchTaskParams params = K8sWatchTaskParams.newBuilder().setK8SResourceKind("Pod").build();

    id = watchServiceDelegate.create(params);
    closeLatch.await(1, TimeUnit.MINUTES);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently has no clearly-defined assertion.")
  public void testList() {
    List<String> watchList = watchServiceDelegate.list();
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently has no clearly-defined assertion.")
  public void testRemove() {
    watchServiceDelegate.delete(id);
  }
}