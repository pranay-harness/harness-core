package io.harness.batch.processing.service.intfc;

import java.io.IOException;

public interface BillingDataPipelineHealthStatusService { void processAndUpdateHealthStatus() throws IOException; }
