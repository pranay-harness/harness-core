package tail

import (
	"context"
	"fmt"
	"time"

	"github.com/hpcloud/tail"
	"go.uber.org/zap"
)

// Start method starts tail on a file.
func Start(ctx context.Context, fileName string, additionalFields map[string]string, disallowJSON bool, log *zap.SugaredLogger) error {
	var err error
	var t *tail.Tail

	fileMapping := FileTailMapping()

	// Disallow multiple requests for tail with the same file name
	fileMapping.mu.Lock()
	_, ok := fileMapping.m[fileName]
	if !ok {
		// Enable tailing on the file and discard logs of the tail package
		// This can be made more powerful by setting ReOpen: true which will tail even for recreated
		// and renamed files. With that parameter set, the watcher will not exit even if the file has been
		// deleted. There is no requirement for it right now.
		t, err = tail.TailFile(fileName, tail.Config{Follow: true, Logger: tail.DiscardingLogger})
		if err == nil {
			fileMapping.m[fileName] = &tailValue{tail: t, startTime: time.Now()}
		}
	}
	fileMapping.mu.Unlock()

	if ok {
		// File has already been tailed. Use this as a fail-safe in case we get bombarded by requests
		// for the same file. There is no reason the same file should be tailed multiple times without
		// tailing being gracefully stopped through `StopTail`
		return nil
	}

	if err != nil {
		return err
	}

	go func() {
		// This loop exits if either the file gets deleted or a stop signal is received
		// In either case, we remove the file mappings and do a cleanup
		for key, val := range additionalFields {
			log = log.With(key, val)
		}

		for line := range t.Lines {
			// If disallowJSON is set as true, emit the logs as they are
			if disallowJSON {
				fmt.Println(line.Text)
				continue
			}
			log.Infof(line.Text)
		}
		t.Cleanup()

		// Remove tail object from map
		fileMapping.mu.Lock()
		delete(fileMapping.m, fileName)
		fileMapping.mu.Unlock()
	}()

	return nil
}

// Stop method stops tail on a file.
func Stop(ctx context.Context, fileName string, wait bool, log *zap.SugaredLogger) error {
	fileMapping := FileTailMapping()

	fileMapping.mu.RLock()
	val, ok := fileMapping.m[fileName]
	fileMapping.mu.RUnlock()
	if !ok {
		// No mapping exists. This is possible if the file was deleted before sending a stop signal
		// or multiple goroutines with the same filename were invoked
		return nil
	}

	// Send stop signal to goroutine tailing on the file
	// If wait is true, wait until entire file has been logged before returning
	// Make sure there has been atleast 1 second gap b/w starting and stopping tailing.
	// TODO: CI-151 to optimise this
	if time.Now().Sub(val.startTime).Seconds() <= 1 {
		time.Sleep(1 * time.Second)
	}

	if !wait {
		go val.tail.StopAtEOF()
	} else {
		val.tail.StopAtEOF()
	}

	return nil
}
