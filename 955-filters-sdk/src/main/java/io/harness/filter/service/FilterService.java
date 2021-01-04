package io.harness.filter.service;

import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;

import java.util.List;
import org.springframework.data.domain.Page;

public interface FilterService {
  FilterDTO create(String accountId, FilterDTO filterDTO);
  FilterDTO update(String accountId, FilterDTO filterDTO);
  boolean delete(String accountId, String orgIdentifier, String projectIdentifier, String identifier, FilterType type);
  FilterDTO get(String accountId, String orgIdentifier, String projectIdentifier, String identifier, FilterType type);
  Page<FilterDTO> list(int page, int size, String accountId, String orgIdentifier, String projectIdentifier,
      List<String> filterIds, FilterType type);
}
