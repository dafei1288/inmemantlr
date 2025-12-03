package org.snt.inmemantlr.comp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Arrays;

/**
 * Builder for configuring Java compiler options passed to the in-memory compiler.
 * Keep options minimal and in the same order they would appear on a javac CLI.
 */
public class CompilerOptionsBuilder {
    private DefaultInnerComplierOptionsProvider optionsProvider = null;

    // Supported classpath flags (synonyms). We normalize to -classpath when adding new.
    private static final List<String> CLASSPATH_FLAGS = Arrays.asList(
            "-classpath", "-cp", "--class-path", "--classpath"
    );

    private static int findClasspathFlagIndex(List<String> opts) {
        for (String f : CLASSPATH_FLAGS) {
            int i = opts.indexOf(f);
            if (i >= 0) return i;
        }
        return -1;
    }

    private CompilerOptionsBuilder(){
        optionsProvider = new DefaultInnerComplierOptionsProvider();
    }

    public static CompilerOptionsBuilder builder(){
        CompilerOptionsBuilder compilerOptionsBuilder = new CompilerOptionsBuilder();
        return compilerOptionsBuilder;
    }

    /**
     * Add or extend the classpath option.
     * If a classpath flag is not present, add "-classpath" and the provided value.
     * If present (in any synonymous form), append the new value using the platform path separator.
     */
    public CompilerOptionsBuilder buildClasspath(String classpath){
        if (classpath == null || classpath.isEmpty()) {
            return this;
        }

        // Access the underlying list so we can find/modify the value next to the flag
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) optionsProvider.getOptions();
        final String defaultFlag = "-classpath"; // normalized when adding

        int idx = findClasspathFlagIndex(opts);
        if (idx < 0) {
            // No existing classpath flag: add normalized flag and value
            optionsProvider.addOption(defaultFlag);
            optionsProvider.addOption(classpath);
            return this;
        }

        // Existing classpath flag: merge the new entry into the value following the flag
        int valueIndex = idx + 1;
        String sep = java.io.File.pathSeparator; // ";" on Windows, ":" on *nix
        if (valueIndex >= opts.size()) {
            // Malformed options where flag is last; insert the value
            opts.add(valueIndex, classpath);
        } else {
            String old = opts.get(valueIndex);
            if (old == null || old.isEmpty()) {
                opts.set(valueIndex, classpath);
            } else if (old.endsWith(sep)) {
                opts.set(valueIndex, old + classpath);
            } else {
                opts.set(valueIndex, old + sep + classpath);
            }
        }
        return this;
    }

    /**
     * Prefer using --release on JDK 9+ to ensure language level, bytecode target and JDK API
     * are aligned. Falls back to -source/-target on older JDKs.
     */
    public CompilerOptionsBuilder buildRelease(String release){
        if (release == null || release.isEmpty()) return this;
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) optionsProvider.getOptions();
        if (supportsRelease()) {
            // Remove -source/-target if present to avoid conflicts
            removeFlagAndValue(opts, "-source");
            removeFlagAndValue(opts, "-target");
            setOrReplace(opts, "--release", release);
        } else {
            // JDK 8 and below: fallback to -source/-target
            setOrReplace(opts, "-source", release);
            setOrReplace(opts, "-target", release);
        }
        return this;
    }

    public CompilerOptionsBuilder buildSource(String jdkVersion){
        if (jdkVersion == null || jdkVersion.isEmpty()) return this;
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) optionsProvider.getOptions();
        // If --release is already set, prefer keeping it single source of truth
        if (opts.contains("--release")) return this;
        setOrReplace(opts, "-source", jdkVersion);
        return this;
    }

    public CompilerOptionsBuilder buildTarget(String jdkVersion){
        if (jdkVersion == null || jdkVersion.isEmpty()) return this;
        @SuppressWarnings("unchecked")
        List<String> opts = (List<String>) optionsProvider.getOptions();
        if (opts.contains("--release")) return this;
        setOrReplace(opts, "-target", jdkVersion);
        return this;
    }

    // Helpers
    private static boolean supportsRelease() {
        // java.specification.version returns 1.8 on JDK 8, 9/11/17 on newer
        String v = System.getProperty("java.specification.version", "");
        try {
            int major = v.contains(".") ? Integer.parseInt(v.substring(v.indexOf('.') + 1))
                                         : Integer.parseInt(v);
            return major >= 9;
        } catch (Exception e) {
            return false;
        }
    }

    private static void setOrReplace(List<String> opts, String flag, String value) {
        int idx = opts.indexOf(flag);
        if (idx >= 0) {
            if (idx + 1 < opts.size()) opts.set(idx + 1, value);
            else opts.add(value);
        } else {
            opts.add(flag);
            opts.add(value);
        }
    }

    private static void removeFlagAndValue(List<String> opts, String flag) {
        int idx = opts.indexOf(flag);
        if (idx >= 0) {
            opts.remove(idx);
            if (idx < opts.size()) {
                // remove the value that followed the flag
                opts.remove(idx);
            }
        }
    }

    /** Return the underlying CompilerOptionsProvider to be used by the compiler. */
    public CompilerOptionsProvider toProvider(){
        return optionsProvider;
    }

    public static class DefaultInnerComplierOptionsProvider implements CompilerOptionsProvider {
        private final Collection<String> optionList = new ArrayList<>();

        public Collection<String> getClassPath() {
            return optionList;
        }

        public void setClassPath(Collection<String> cp) {
            this.optionList.addAll(cp);
        }

        /** Add one option token as it would appear on the javac command line. */
        public void addOption(String option){
            optionList.add(option);
        }

        @Override
        public Collection<String> getOptions() {
            return optionList;
        }
    }
}