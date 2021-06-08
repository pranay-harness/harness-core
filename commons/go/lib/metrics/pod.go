package metrics

import (
	"context"
	"errors"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	_ "k8s.io/client-go/plugin/pkg/client/auth"
	"k8s.io/client-go/tools/clientcmd"
	metrics "k8s.io/metrics/pkg/client/clientset/versioned"
	"go.uber.org/zap"
)

// ContainerResource is the resource taken by a container
type ContainerResource struct {
	MemoryMib int64
	MilliCPU  int64
}

func GetPodMetrics(podName, namespace string, log *zap.SugaredLogger) (map[string]ContainerResource, error) {
	ctx := context.Background()

	var kubeconfig, master string //empty, assuming inClusterConfig
	config, err := clientcmd.BuildConfigFromFlags(master, kubeconfig)
	if err != nil {
		return nil, err
	}

	mc, err := metrics.NewForConfig(config)
	if err != nil {
		return nil, err
	}
	podMetric, err := mc.MetricsV1beta1().PodMetricses(namespace).Get(ctx, podName, metav1.GetOptions{})
	if err != nil {
		return nil, err
	}

	log.Infow("Pod metric returned", "metric", podMetric)

	podContainers := podMetric.Containers
	ctrResourceByName := make(map[string]ContainerResource)
	for _, container := range podContainers {
		memQuantityBytes, ok := container.Usage.Memory().AsInt64() // In bytes
		if !ok {
			return nil, errors.New("failed to get memory")
		}

		cpu := (int64)(container.Usage.Cpu().AsApproximateFloat64() * 1000)
		memory := (int64)(memQuantityBytes / (1024 * 1024))

		ctrResourceByName[container.Name] = ContainerResource{
			MemoryMib: memory,
			MilliCPU:  cpu,
		}
	}
	return ctrResourceByName, nil
}
