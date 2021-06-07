package metrics

import (
	"sync"

	cmetrics "github.com/wings-software/portal/commons/go/lib/metrics"
)

var (
	s    *metricsState
	once sync.Once
)

type metricsState struct {
	mu                 sync.Mutex
	ctrResourcesByName map[string]cmetrics.ContainerResource // Store max resources consumed by a container
}

// Updates the container max resources
func (s *metricsState) Update(currCtrResourceByName map[string]cmetrics.ContainerResource) {
	s.mu.Lock()
	defer s.mu.Unlock()

	for ctrName, currResource := range currCtrResourceByName {
		if maxResource, ok := s.ctrResourcesByName[ctrName]; ok {
			if currResource.MemoryMib > maxResource.MemoryMib {
				maxResource.MemoryMib = currResource.MemoryMib
			}
			if currResource.MilliCPU > maxResource.MilliCPU {
				maxResource.MilliCPU = currResource.MilliCPU
			}
		} else {
			s.ctrResourcesByName[ctrName] = currResource
		}
	}
}

func (s *metricsState) Get(ctrName string) (int64, int64) {
	if maxResource, ok := s.ctrResourcesByName[ctrName]; ok {
		return maxResource.MemoryMib, maxResource.MilliCPU
	}

	return -1, -1
}

// ExecutionState returns execution state
func MetricState() *metricsState {
	once.Do(func() {
		s = &metricsState{}
		s.ctrResourcesByName = make(map[string]cmetrics.ContainerResource)
	})
	return s
}
