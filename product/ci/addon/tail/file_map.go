package tail

import (
	"sync"
	"time"

	"github.com/hpcloud/tail"
)

type tailValue struct {
	tail      *tail.Tail
	startTime time.Time
}

type fileTailMapping struct {
	m  map[string]*tailValue
	mu sync.RWMutex
}

var (
	f    *fileTailMapping
	once sync.Once
)

func FileTailMapping() *fileTailMapping {
	once.Do(func() {
		f = &fileTailMapping{
			m: make(map[string]*tailValue),
		}
	})

	return f
}
