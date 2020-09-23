package memory

import (
	"context"
	"sync"
	"testing"

	"github.com/wings-software/portal/product/log-service/stream"

	"github.com/google/go-cmp/cmp"
)

func TestStreamer(t *testing.T) {
	s := New()
	err := s.Create(context.Background(), "1")
	if err != nil {
		t.Error(err)
	}
	if len(s.streams) == 0 {
		t.Errorf("Want stream registered")
	}

	w := sync.WaitGroup{}
	w.Add(4)
	go func() {
		s.Write(context.Background(), "1", &stream.Line{})
		s.Write(context.Background(), "1", &stream.Line{})
		s.Write(context.Background(), "1", &stream.Line{})
		w.Done()
	}()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	tail, errc := s.Tail(ctx, "1")

	go func() {
		for {
			select {
			case <-errc:
				return
			case <-ctx.Done():
				return
			case <-tail:
				w.Done()
			}
		}
	}()

	w.Wait()
}

func TestStreamerDelete(t *testing.T) {
	s := New()
	err := s.Create(context.Background(), "1")
	if err != nil {
		t.Error(err)
	}
	if len(s.streams) == 0 {
		t.Errorf("Want stream registered")
	}
	err = s.Delete(context.Background(), "1")
	if err != nil {
		t.Error(err)
	}
	if len(s.streams) != 0 {
		t.Errorf("Want stream unregistered")
	}
}

func TestStreamerDeleteErr(t *testing.T) {
	s := New()
	err := s.Delete(context.Background(), "1")
	if err != stream.ErrNotFound {
		t.Errorf("Want errStreamNotFound")
	}
}

func TestStreamerWriteErr(t *testing.T) {
	s := New()
	err := s.Write(context.Background(), "1", &stream.Line{})
	if err != stream.ErrNotFound {
		t.Errorf("Want errStreamNotFound")
	}
}

func TestStreamTailNotFound(t *testing.T) {
	s := New()
	outc, errc := s.Tail(context.Background(), "0")
	if outc != nil && errc != nil {
		t.Errorf("Expect nil channel when stream not found")
	}
}

func TestStreamerInfo(t *testing.T) {
	s := New()
	s.streams["1"] = &memoryStream{list: map[*subscriber]struct{}{{}: {}, {}: {}}}
	s.streams["2"] = &memoryStream{list: map[*subscriber]struct{}{{}: {}}}
	s.streams["3"] = &memoryStream{list: map[*subscriber]struct{}{}}
	got := s.Info(context.Background())

	want := &stream.Info{
		Streams: map[string]stream.Stats{
			"1": {Subs: 2},
			"2": {Subs: 1},
			"3": {Subs: 0},
		},
	}

	if diff := cmp.Diff(got, want); diff != "" {
		t.Errorf(diff)
	}
}
