package metrics

import (
	"time"

	cmetrics "github.com/wings-software/portal/commons/go/lib/metrics"
	"go.uber.org/zap"
)

// Collect container metrics periodically for a pod
func Collect(podName, namespace string, log *zap.SugaredLogger) {
	go func() {
		for {
			ctrResourcesByName, err := cmetrics.GetPodMetrics(podName, namespace)
			if err != nil {
				log.Infow("failed to fetch pod metrics", zap.Error(err))
				time.Sleep(time.Second * 5)
				continue
			}

			log.Infow("Current pod metrics", "metrics", ctrResourcesByName)
			ms := MetricState()
			ms.Update(ctrResourcesByName)
			log.Infow("Updated max pod metrics resource", "max_resources", ms.ctrResourcesByName)
			time.Sleep(time.Second * 5)
		}
	}()
}
