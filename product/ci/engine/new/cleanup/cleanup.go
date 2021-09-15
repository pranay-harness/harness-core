package cleanup

import (
	"context"
	"fmt"

	"github.com/pkg/errors"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

var (
	newAddonClient = caddon.NewAddonClient
)

// Stops all the containers by stopping grpc servers running on them.
func stopPod(ctx context.Context, ports []uint, stopCh chan bool, log *zap.SugaredLogger) {
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

	addonClient, err := newAddonClient(port, log)
	if err != nil {
		log.Errorw("could not create CI Addon client", "port", port, zap.Error(err))
		return errors.Wrap(err, "could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	_, err = addonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		log.Errorw("unable to send Stop server request to addon",
			"port", port, zap.Error(err))
		return errors.Wrap(err, "could not send stop request")
	}

	log.Infow(fmt.Sprintf("Stopped grpc server successfully on port %d", port))
	return nil
}
