package io.harness.batch.processing.entities;

import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.event.grpc.PublishedMessage;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public class BatchProcessingMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(InstanceData.class, PublishedMessage.class, BatchJobScheduledData.class);

  private BatchProcessingMorphiaClasses() {}
}
