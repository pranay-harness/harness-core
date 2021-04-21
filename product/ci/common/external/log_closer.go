package external

import (
	"fmt"
	"os"
	"os/signal"
	"sync"
	"syscall"

	"github.com/wings-software/portal/commons/go/lib/logs"
)

var (
	lc   *logCloser
	once sync.Once
)

// Gracefully closes all the open remote log streams in case of SIGTERM
// SIGTERM signal is issued when a pod is deleted.
// Kubernetes waits for upto 30 seconds before terminating the pod

type logCloser struct {
	rls chan *logs.RemoteLogger
}

func (lc *logCloser) Add(rl *logs.RemoteLogger) {
	lc.rls <- rl
}

// Waits for the SIGTERM signal and closes all the open remote loggers
func (lc *logCloser) Run() {
	ch := make(chan os.Signal, 1)
	signal.Notify(ch, syscall.SIGTERM)

	go func() {
		sig := <-ch
		fmt.Printf("Received signal: %s. Closing all the remote loggers", sig)
		for rl := range lc.rls {
			if err := rl.Writer.Close(); err != nil {
				fmt.Printf("failed to close remote logger with err: %v", err)
			}
		}
	}()
}

// LogCloser returns log closer to handle graceful shutdown of log stream
// in case of SIGTERM signal
func LogCloser() *logCloser {
	once.Do(func() {
		lc = &logCloser{
			rls: make(chan *logs.RemoteLogger, 5),
		}
	})
	return lc
}
