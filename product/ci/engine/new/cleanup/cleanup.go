package cleanup

import (
	"context"
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/new/state"
	"go.uber.org/zap"
)

const (
	cleanupWaitDuration = 60 * time.Second
	maxAddonRetries     = 5
)

var (
	newAddonClient = caddon.NewAddonClient
)

// RunCleanupJob cleans up the pod if there is no execution running for a duration of cleanupWaitDuration.
func RunCleanupJob(ports []uint, stopCh chan bool, log *zap.SugaredLogger) {
	lastRunningExecutionTime := time.Now()
	for {
		s := state.ExecutionState()
		if s.CancelCleanupJob() {
			log.Infow("Stopping pod cleanup job.")
			break
		}
		currTime := time.Now()
		if s.IsAnyExecutionRunning() {
			lastRunningExecutionTime = currTime
		}

		if time.Since(lastRunningExecutionTime) > cleanupWaitDuration {
			log.Infow("No running execution since cleanupWaitDuration. Cleaning up the pod.")
			stopPod(ports, stopCh, log)
			break
		}

		time.Sleep(60 * time.Second)
	}
}

// Stops all the containers by stopping grpc servers running on them.
func stopPod(ports []uint, stopCh chan bool, log *zap.SugaredLogger) {
	ctx := context.Background()
	for _, port := range ports {
		stopContainer(ctx, port, log)
	}
	log.Infow("Sending signal to stop lite-engine server")
	stopCh <- true
	log.Infow("Sent signal to stop lite-engine server")
}

// Stops the grpc server running on the specified port.
// This will stop the container where grpc service is running on the specified port.
func stopContainer(ctx context.Context, port uint, log *zap.SugaredLogger) error {
	log.Infow(fmt.Sprintf("Stopping grpc server running on port %d", port))

	client, err := newAddonClient(port, log)
	if err != nil {
		log.Errorw("could not create CI Addon client", "port", port, zap.Error(err))
		return errors.Wrap(err, "could not create CI Addon client")
	}
	defer client.CloseConn()

	_, err = client.Client().SignalStop(ctx, &addonpb.SignalStopRequest{}, grpc_retry.WithMax(maxAddonRetries))
	if err != nil {
		log.Errorw("unable to send Stop server request to addon",
			"port", port, zap.Error(err))
		return errors.Wrap(err, "could not send stop request")
	}

	log.Infow(fmt.Sprintf("Stopped grpc server successfully on port %d", port))
	return nil
}
