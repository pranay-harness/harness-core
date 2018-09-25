package io.harness.mongo;

import static io.harness.persistence.ReadPref.CRITICAL;
import static io.harness.persistence.ReadPref.NORMAL;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import org.mongodb.morphia.AdvancedDatastore;

import java.util.Map;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class MongoPersistence implements HPersistence {
  protected AdvancedDatastore primaryDatastore;
  protected AdvancedDatastore secondaryDatastore;
  protected Map<ReadPref, AdvancedDatastore> datastoreMap;

  public MongoPersistence(AdvancedDatastore primaryDatastore, AdvancedDatastore secondaryDatastore) {
    this.primaryDatastore = primaryDatastore;
    this.secondaryDatastore = secondaryDatastore;
    this.datastoreMap = ImmutableMap.of(NORMAL, secondaryDatastore, CRITICAL, primaryDatastore);
  }
}
