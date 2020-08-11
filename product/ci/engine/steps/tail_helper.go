// Helper package to start/stop tail on a file

package steps

import (
	"context"
	"fmt"

	"github.com/pkg/errors"
	aclient "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

func StartTail(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
	if filename == "" {
		log.Warnw("No file name specified for starting tail")
		return errors.New("No file name specified")
	}
	addonClient, err := newAddonClient(aclient.AddonPort, log)
	if err != nil {
		return err
	}
	defer addonClient.CloseConn()

	arg := &addonpb.StartTailRequest{FileName: filename, AdditionalFields: additionalFields}

	c := addonClient.Client()
	_, err = c.StartTail(ctx, arg)
	if err != nil {
		log.Warnw(fmt.Sprintf("Could not start tail on file %s", arg.GetFileName()))
		return err
	}
	return nil
}

func StopTail(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
	if filename == "" {
		log.Warnw("No file name specified for stopping tail")
		return errors.New("No file name specified")
	}
	addonClient, err := newAddonClient(aclient.AddonPort, log)
	if err != nil {
		return err
	}
	defer addonClient.CloseConn()

	arg := &addonpb.StopTailRequest{FileName: filename, Wait: wait}

	c := addonClient.Client()
	_, err = c.StopTail(ctx, arg)
	if err != nil {
		log.Warnw(fmt.Sprintf("Could not stop tail on file %s", arg.GetFileName()))
		return err
	}
	return nil
}
