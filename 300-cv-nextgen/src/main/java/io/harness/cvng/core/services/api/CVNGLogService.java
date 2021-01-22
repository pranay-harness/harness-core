package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.cvnglog.CVNGLogDTO;

import java.util.List;

public interface CVNGLogService {
  void save(List<CVNGLogDTO> callLogs);
}
