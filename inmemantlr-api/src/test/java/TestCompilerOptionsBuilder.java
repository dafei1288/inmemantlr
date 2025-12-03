import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.snt.inmemantlr.comp.CompilerOptionsBuilder;
import org.snt.inmemantlr.comp.CompilerOptionsProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestCompilerOptionsBuilder {

    private static boolean supportsRelease() {
        String v = System.getProperty("java.specification.version", "");
        try {
            int major = v.contains(".") ? Integer.parseInt(v.substring(v.indexOf('.') + 1))
                    : Integer.parseInt(v);
            return major >= 9;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    public void testToProviderAndOptions() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder()
                .buildClasspath("A")
                .buildSource("17");
        CompilerOptionsProvider p = b.toProvider();
        Assertions.assertNotNull(p);
        Collection<String> opts = p.getOptions();
        Assertions.assertTrue(opts.contains("-classpath") || opts.contains("-cp")
                || opts.contains("--class-path") || opts.contains("--classpath"));
        Assertions.assertTrue(opts.contains("-source") || opts.contains("--release"));
    }

    @Test
    public void testBuildReleasePrefersRelease() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder()
                .buildRelease("8");
        List<String> opts = new ArrayList<>(b.toProvider().getOptions());
        if (supportsRelease()) {
            int i = opts.indexOf("--release");
            Assertions.assertTrue(i >= 0);
            Assertions.assertEquals("8", opts.get(i + 1));
            Assertions.assertFalse(opts.contains("-source"));
            Assertions.assertFalse(opts.contains("-target"));
        } else {
            int s = opts.indexOf("-source");
            int t = opts.indexOf("-target");
            Assertions.assertTrue(s >= 0 && t >= 0);
            Assertions.assertEquals("8", opts.get(s + 1));
            Assertions.assertEquals("8", opts.get(t + 1));
        }
    }

    @Test
    public void testReleaseThenSourceTarget() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder()
                .buildRelease("11")
                .buildSource("17")
                .buildTarget("17");
        List<String> opts = new ArrayList<>(b.toProvider().getOptions());
        if (supportsRelease()) {
            int i = opts.indexOf("--release");
            Assertions.assertTrue(i >= 0);
            Assertions.assertEquals("11", opts.get(i + 1));
            Assertions.assertFalse(opts.contains("-source"));
            Assertions.assertFalse(opts.contains("-target"));
        } else {
            int s = opts.indexOf("-source");
            int t = opts.indexOf("-target");
            Assertions.assertTrue(s >= 0 && t >= 0);
            Assertions.assertEquals("17", opts.get(s + 1));
            Assertions.assertEquals("17", opts.get(t + 1));
        }
    }

    @Test
    public void testClasspathAppendDefaultFlag() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder()
                .buildClasspath("FOO")
                .buildClasspath("BAR");
        List<String> opts = (List<String>) b.toProvider().getOptions();
        int i = opts.indexOf("-classpath");
        Assertions.assertTrue(i >= 0);
        String expected = "FOO" + File.pathSeparator + "BAR";
        Assertions.assertEquals(expected, opts.get(i + 1));
    }

    @Test
    public void testClasspathAppendOnCpSynonym() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder();
        List<String> opts = (List<String>) b.toProvider().getOptions();
        opts.add("-cp");
        opts.add("X");
        b.buildClasspath("Y");
        int i = opts.indexOf("-cp");
        Assertions.assertTrue(i >= 0);
        Assertions.assertEquals("X" + File.pathSeparator + "Y", opts.get(i + 1));
        // Ensure no extra -classpath flag added
        Assertions.assertEquals(-1, opts.indexOf("-classpath"));
    }

    @Test
    public void testClasspathAppendOnLongSynonym() {
        CompilerOptionsBuilder b = CompilerOptionsBuilder.builder();
        List<String> opts = (List<String>) b.toProvider().getOptions();
        opts.add("--class-path");
        opts.add("P");
        b.buildClasspath("Q");
        int i = opts.indexOf("--class-path");
        Assertions.assertTrue(i >= 0);
        Assertions.assertEquals("P" + File.pathSeparator + "Q", opts.get(i + 1));
        Assertions.assertEquals(-1, opts.indexOf("-classpath"));
    }
}