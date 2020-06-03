package io.harness.batch.processing.entities;

import com.google.common.collect.ImmutableSet;

import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.event.grpc.PublishedMessage;

import java.util.Set;

public class BatchProcessingMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(InstanceData.class, PublishedMessage.class, BatchJobScheduledData.class);

  private BatchProcessingMorphiaClasses() {}
}
