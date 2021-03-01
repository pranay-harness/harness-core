package io.harness.callgraph.trampoline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class TrampolineAgent {

    public static void premain(String arg, Instrumentation inst) {
        try {
            inst.appendToBootstrapClassLoaderSearch(new JarFile(toFile("boot")));
            ClassLoader loader = new URLClassLoader(new URL[] {toFile("agent").toURI().toURL()}, ClassLoader.getSystemClassLoader().getParent());
            Class.forName("io.harness.callgraph.instr.Tracer", false, loader)
                    .getMethod("premain", String.class, Instrumentation.class)
                    .invoke(null, arg, inst);
        } catch (Throwable t) {
            t.printStackTrace(); // No infra available from here, no proper logging possible.
        }
    }

    private static File toFile(String resource) throws IOException {
        InputStream inputStream = TrampolineAgent.class.getResourceAsStream("/callgraph-" + resource + ".jar");
        try {
            File boot = File.createTempFile("callgraph-" + resource, ".jar");
            FileOutputStream outputStream = new FileOutputStream(boot);
            try {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
            } finally {
                outputStream.close();
            }
            return boot;
        } finally {
            inputStream.close();
        }
    }
}
