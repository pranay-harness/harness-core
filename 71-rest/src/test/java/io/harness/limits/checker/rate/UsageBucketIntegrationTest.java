package io.harness.limits.checker.rate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.integration.IntegrationTestUtil;

import java.util.Arrays;

public class UsageBucketIntegrationTest extends BaseIntegrationTest {
  @Inject private WingsPersistence persistence;

  private boolean indexesEnsured;

  private static final String KEY = "some-id-" + UsageBucketIntegrationTest.class.getSimpleName();

  @Before
  public void ensureIndices() throws Exception {
    if (!indexesEnsured && !IntegrationTestUtil.isManagerRunning(client)) {
      persistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL).ensureIndexes(UsageBucket.class);
      indexesEnsured = true;
    }
  }

  @After
  public void cleanUp() throws Exception {
    persistence.getDatastore(HPersistence.DEFAULT_STORE, ReadPref.NORMAL)
        .delete(persistence.createQuery(UsageBucket.class).filter("key", KEY));
  }

  @Test
  public void testSerialization() {
    UsageBucket bucket = new UsageBucket(KEY, Arrays.asList(10L, 11L, 101L, 102L));

    String id = persistence.save(bucket);
    assertNotNull(id);

    UsageBucket fetchedBucket = persistence.get(UsageBucket.class, id);
    assertEquals(bucket, fetchedBucket);
  }
}
