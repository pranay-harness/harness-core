// Package metrics logs the memory & cpu statistics of a process periodically
package metrics

import (
	"time"

	"github.com/shirou/gopsutil/v3/process"
	"go.uber.org/zap"
)

// Log method periodically logs the memory & cpu statistics of a process
func Log(pid int32, id string, log *zap.SugaredLogger) {
	p, err := process.NewProcess(pid)
	if err != nil {
		log.Errorw("failed to start resource monitoring", "pid", pid, "id", id, zap.Error(err))
		return
	}

	go func() {
		for {
			memInfo, err := p.MemoryInfo()
			if err != nil {
				log.Errorw("failed to get memory info", "pid", pid, "id", id, zap.Error(err))
				return
			}
			cpu, err := p.CPUPercent()
			if err != nil {
				log.Errorw("failed to get cpu info", "pid", pid, "id", id, zap.Error(err))
				return
			}

			log.Infow("resources utilized", "pid", pid, "id", id, "rss_memory_mb", memInfo.RSS/(1024*1024), "cpu_percent", cpu)
			time.Sleep(time.Second * 30)
		}
	}()
}
