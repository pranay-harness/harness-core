package io.harness.batch.processing.metrics;

import java.util.List;

public interface CENGTelemetryService {
  List<CEConnectorsTelemetry> getNextGenConnectorsCountByType(String accountId);
}
