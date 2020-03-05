package io.harness.batch.processing.config;

import com.google.common.collect.ImmutableList;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.CostEventSource;
import io.harness.batch.processing.ccm.CostEventType;
import io.harness.batch.processing.ccm.EnrichedEvent;
import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.data.CostEventData.CostEventDataBuilder;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.batch.processing.processor.support.K8sLabelServiceInfoFetcher;
import io.harness.batch.processing.reader.EventReaderFactory;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.ccm.cluster.dao.K8sYamlDao;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.event.grpc.PublishedMessage;
import io.harness.govern.Switch;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class K8sWatchEventConfig {
  private static final int BATCH_SIZE = 100;
  private final EventReaderFactory eventReaderFactory;
  private final StepBuilderFactory stepBuilderFactory;

  public K8sWatchEventConfig(
      @Qualifier("mongoEventReader") EventReaderFactory eventReaderFactory, StepBuilderFactory stepBuilderFactory) {
    this.eventReaderFactory = eventReaderFactory;
    this.stepBuilderFactory = stepBuilderFactory;
  }

  @Bean
  @StepScope
  public ItemReader<PublishedMessage> reader(@Value("#{jobParameters[accountId]}") String accountId,
      @Value("#{jobParameters[startDate]}") Long startDate, @Value("#{jobParameters[endDate]}") Long endDate) {
    return eventReaderFactory.getEventReader(accountId, EventTypeConstants.K8S_WATCH_EVENT, startDate, endDate);
  }

  @Bean
  public ItemProcessor<PublishedMessage, PublishedMessage> normalizer(K8sYamlDao k8sYamlDao) {
    return k8sWatchEventMsg -> {
      K8sWatchEvent k8sWatchEvent = (K8sWatchEvent) k8sWatchEventMsg.getMessage();
      // special handling for added events (for restarting watches)
      if (k8sWatchEvent.getType() == K8sWatchEvent.Type.TYPE_ADDED) {
        K8sYaml k8sYaml =
            k8sYamlDao.fetchLatestYaml(k8sWatchEvent.getClusterId(), k8sWatchEvent.getResourceRef().getUid());
        if (k8sYaml != null) {
          if (k8sWatchEvent.getNewResourceYaml().equals(k8sYaml.getYaml())) {
            // same yaml already captured - skip.
            return null;
          }
          // latest captured yaml is different - convert to updated event.
          return k8sWatchEventMsg.toBuilder()
              .message(K8sWatchEvent.newBuilder(k8sWatchEvent)
                           .setType(K8sWatchEvent.Type.TYPE_UPDATED)
                           .setOldResourceYaml(k8sYaml.getYaml())
                           .setOldResourceVersion(k8sYaml.getResourceVersion())
                           .setDescription("Missed update event")
                           .build())
              .build();
        }
        // No pre-existing yaml => true ADDED
        return k8sWatchEventMsg;
      }
      return k8sWatchEventMsg;
    };
  }

  @Bean
  public ItemProcessor<PublishedMessage, EnrichedEvent<K8sWatchEvent>> enricher(
      WorkloadRepository workloadRepository, K8sLabelServiceInfoFetcher k8sLabelServiceInfoFetcher) {
    return k8sWatchEventMsg -> {
      String accountId = k8sWatchEventMsg.getAccountId();
      K8sWatchEvent k8sWatchEvent = (K8sWatchEvent) k8sWatchEventMsg.getMessage();
      String clusterId = k8sWatchEvent.getClusterId();
      String uid = k8sWatchEvent.getResourceRef().getUid();
      Map<String, String> labels = workloadRepository.getWorkload(accountId, clusterId, uid)
                                       .map(K8sWorkload::getLabels)
                                       .orElse(Collections.emptyMap());
      Optional<HarnessServiceInfo> serviceInfo = k8sLabelServiceInfoFetcher.fetchHarnessServiceInfo(accountId, labels);
      return new EnrichedEvent<>(accountId, k8sWatchEventMsg.getOccurredAt(), k8sWatchEvent, serviceInfo.orElse(null));
    };
  }

  @Bean
  public ItemProcessor<PublishedMessage, EnrichedEvent<K8sWatchEvent>> processor(
      ItemProcessor<PublishedMessage, PublishedMessage> normalizer,
      ItemProcessor<PublishedMessage, EnrichedEvent<K8sWatchEvent>> enricher) {
    return new CompositeItemProcessorBuilder<PublishedMessage, EnrichedEvent<K8sWatchEvent>>()
        .delegates(ImmutableList.of(normalizer, enricher))
        .build();
  }

  @Bean
  public ItemWriter<EnrichedEvent<K8sWatchEvent>> writer(K8sYamlDao k8sYamlDao, CostEventService costEventService) {
    return enrichedK8sEvents
        -> costEventService.create(
            enrichedK8sEvents.stream()
                .map(enrichedK8sEvent -> {
                  String oldYamlRef = null;
                  String newYamlRef = null;
                  K8sWatchEvent k8sWatchEvent = enrichedK8sEvent.getEvent();
                  K8sWatchEvent.Type watchEventType = k8sWatchEvent.getType();
                  switch (watchEventType) {
                    case TYPE_ADDED:
                      newYamlRef = k8sYamlDao.ensureYamlSaved(k8sWatchEvent.getClusterId(),
                          k8sWatchEvent.getResourceRef().getUid(), k8sWatchEvent.getNewResourceVersion(),
                          k8sWatchEvent.getNewResourceYaml());
                      break;
                    case TYPE_UPDATED:
                      oldYamlRef = k8sYamlDao.ensureYamlSaved(k8sWatchEvent.getClusterId(),
                          k8sWatchEvent.getResourceRef().getUid(), k8sWatchEvent.getOldResourceVersion(),
                          k8sWatchEvent.getOldResourceYaml());
                      newYamlRef = k8sYamlDao.ensureYamlSaved(k8sWatchEvent.getClusterId(),
                          k8sWatchEvent.getResourceRef().getUid(), k8sWatchEvent.getNewResourceVersion(),
                          k8sWatchEvent.getNewResourceYaml());
                      break;
                    case TYPE_DELETED:
                      oldYamlRef = k8sYamlDao.ensureYamlSaved(k8sWatchEvent.getClusterId(),
                          k8sWatchEvent.getResourceRef().getUid(), k8sWatchEvent.getOldResourceVersion(),
                          k8sWatchEvent.getOldResourceYaml());
                      break;
                    default:
                      Switch.unhandled(watchEventType);
                  }
                  CostEventDataBuilder costEventDataBuilder =
                      CostEventData.builder()
                          .accountId(enrichedK8sEvent.getAccountId())
                          .clusterType(ClusterType.K8S.name())
                          .costEventSource(CostEventSource.K8S_CLUSTER.name())
                          .costEventType(CostEventType.K8S_RESOURCE_CHANGE.name())
                          .clusterId(k8sWatchEvent.getClusterId())
                          .startTimestamp(enrichedK8sEvent.getOccurredAt())
                          .eventDescription(k8sWatchEvent.getDescription())
                          .oldYamlRef(oldYamlRef)
                          .newYamlRef(newYamlRef);
                  if (enrichedK8sEvent.getHarnessServiceInfo() != null) {
                    costEventDataBuilder =
                        costEventDataBuilder.appId(enrichedK8sEvent.getHarnessServiceInfo().getAppId())
                            .serviceId(enrichedK8sEvent.getHarnessServiceInfo().getServiceId())
                            .envId(enrichedK8sEvent.getHarnessServiceInfo().getEnvId())
                            .cloudProviderId(k8sWatchEvent.getCloudProviderId());
                  }
                  return costEventDataBuilder.build();
                })
                .collect(Collectors.toList()));
  }

  @Bean
  public Step k8sWatchEventsStep(ItemReader<? extends PublishedMessage> reader,
      ItemProcessor<? super PublishedMessage, ? extends EnrichedEvent<K8sWatchEvent>> processor,
      ItemWriter<? super EnrichedEvent<K8sWatchEvent>> writer) {
    return stepBuilderFactory.get("k8sWatchEventsStep")
        .<PublishedMessage, EnrichedEvent<K8sWatchEvent>>chunk(BATCH_SIZE)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .build();
  }
  @Bean
  @Autowired
  public Job k8sClusterEventsJob(JobBuilderFactory jobBuilderFactory, Step k8sWatchEventsStep) {
    return jobBuilderFactory.get(BatchJobType.K8S_WATCH_EVENT.name())
        .incrementer(new RunIdIncrementer())
        .start(k8sWatchEventsStep)
        .build();
  }
}
