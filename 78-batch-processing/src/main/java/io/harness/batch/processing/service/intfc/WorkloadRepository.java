package io.harness.batch.processing.service.intfc;

import io.harness.perpetualtask.k8s.watch.PodInfo;

public interface WorkloadRepository { void savePodWorkload(String accountId, PodInfo podInfo); }
