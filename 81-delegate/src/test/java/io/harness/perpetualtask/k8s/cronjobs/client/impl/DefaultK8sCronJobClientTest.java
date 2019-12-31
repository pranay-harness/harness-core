package io.harness.perpetualtask.k8s.cronjobs.client.impl;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.k8s.cronjobs.client.K8sCronJobClient;
import io.harness.perpetualtask.k8s.cronjobs.client.model.CronJob;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DefaultK8sCronJobClientTest extends CategoryTest {
  @Rule public final KubernetesServer server = new KubernetesServer();

  @Before
  public void setUp() throws Exception {
    server.expect()
        .withPath("/apis/batch/v1beta1/namespaces/ns1/cronjobs/cj1")
        .andReturn(200,
            "{\n"
                + "  \"apiVersion\" : \"batch/v1beta1\",\n"
                + "  \"kind\" : \"CronJob\",\n"
                + "  \"metadata\" : {\n"
                + "    \"name\" : \"cj1\",\n"
                + "    \"namespace\" : \"ns1\",\n"
                + "    \"uid\" : \"b73be2c5-43a2-11e9-98a9-4201ac10650b\"\n"
                + "  }\n"
                + "}\n")
        .once();
    server.expect()
        .withPath("/apis/batch/v1beta1/namespaces/ns1/cronjobs")
        .andReturn(200,
            "{\n"
                + "  \"apiVersion\": \"v1\",\n"
                + "  \"items\": [\n"
                + "    {\n"
                + "      \"apiVersion\": \"batch/v1beta1\",\n"
                + "      \"kind\": \"CronJob\",\n"
                + "      \"metadata\": {\n"
                + "        \"name\": \"cj1\",\n"
                + "        \"namespace\": \"ns1\",\n"
                + "        \"uid\": \"b73be2c5-43a2-11e9-98a9-4201ac10650b\"\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"kind\": \"List\",\n"
                + "  \"metadata\": {\n"
                + "    \"resourceVersion\": \"\",\n"
                + "    \"selfLink\": \"\"\n"
                + "  }\n"
                + "}\n")
        .once();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testCronJobs() throws Exception {
    K8sCronJobClient client = server.getClient().adapt(K8sCronJobClient.class);
    CronJob cj = client.cronJobs().inNamespace("ns1").withName("cj1").get();
    assertThat(cj.getKind()).isEqualTo("CronJob");
    assertThat(cj.getMetadata()).satisfies(meta -> {
      assertThat(meta.getName()).isEqualTo("cj1");
      assertThat(meta.getUid()).isEqualTo("b73be2c5-43a2-11e9-98a9-4201ac10650b");
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testListCronJobs() throws Exception {
    K8sCronJobClient client = server.getClient().adapt(K8sCronJobClient.class);
    assertThat(client.cronJobs().inNamespace("ns1").list().getItems()).hasSize(1);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testIsAdaptable() throws Exception {
    assertThat(server.getClient().isAdaptable(K8sCronJobClient.class)).isTrue();
  }
}
