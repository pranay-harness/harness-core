package tasks

import (
	"context"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/metrics"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

var (
	mlog = metrics.Log
)

func runCmd(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
	logMetrics bool, log *zap.SugaredLogger, metricLog *zap.SugaredLogger) error {
	err := cmd.Start()
	if err != nil {
		log.Errorw(
			"error encountered while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(err))
		return err
	}

	if logMetrics {
		pid := cmd.Pid()
		mlog(int32(pid), stepID, metricLog)
	}

	err = cmd.Wait()
	if rusage, e := cmd.ProcessState().SysUsageUnit(); e == nil {
		metricLog.Infow(
			"max RSS memory used by step",
			"step_id", stepID,
			"max_rss_memory_kb", rusage.Maxrss)
	}

	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		log.Errorw(
			"timeout while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(ctxErr))
		return ctxErr
	}

	if err != nil {
		log.Errorw(
			"error encountered while executing the step",
			"step_id", stepID,
			"retry_count", retryCount,
			"commands", commands,
			"elapsed_time_ms", utils.TimeSince(startTime),
			zap.Error(err))
		return err
	}
	return nil
}
