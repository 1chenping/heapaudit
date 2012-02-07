package com.foursquare.heapaudit;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class HeapUtil {

    public static void log(String text) {

        System.out.println(text);

    }

    public static void instrumentation(boolean debug,
                                       String text) {

        if (debug) {

            log("\t" + text);

        }

    }

    public static void execution(boolean trace,
                                 MethodAdapter mv,
                                 String text) {

        if (trace) {

            // STACK [...]
            mv.visitLdcInsn(text);
            // STACK [...|text]
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                               "com/foursquare/heapaudit/HeapUtil",
                               "log",
                               "(Ljava/lang/String;)V");
            // STACK [...]

        }

    }

    protected static void visitCheck(MethodVisitor mv,
                                     Label cleanup) {

        // STACK: [...]
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com/foursquare/heapaudit/HeapRecorder",
                           "hasRecorders",
                           "()Z");
        // STACK: [...|status]
        mv.visitJumpInsn(Opcodes.IFEQ,
                         cleanup);
        // STACK: [...]

    }

    protected static void visitCleanup(MethodVisitor mv,
                                       Label cleanup,
                                       Label finish) {

        // STACK: [...]
        mv.visitJumpInsn(Opcodes.GOTO,
                         finish);
        // STACK: [...]
        mv.visitLabel(cleanup);
        // STACK: [...]

    }

    protected static void visitFinish(MethodVisitor mv,
                                      Label finish) {

        // STACK: [...]
        mv.visitLabel(finish);
        // STACK: [...]

    }

    // The following holds a cache of type to size mappings.

    private static ConcurrentHashMap<String, Long> sizes = new ConcurrentHashMap<String, Long>();

    private static long sizeOf(Object obj,
                               String type) {

        Long size = sizes.get(type);

        if (size == null) {

            size = HeapRecorder.instrumentation.getObjectSize(obj);

            sizes.put(type,
                      size);

        }

        return size;

    }

    // This is a per thread status to ignore allocations performed within the
    // record code path.

    private static ThreadLocal<Integer> recording = new ThreadLocal<Integer>() {

        @Override protected Integer initialValue() {

            return 0;

        }

    };

    public static void record(Object obj,
                              int count,
                              String type,
                              long size) {

        // The following suppresses recording of allocations due to the
        // HeapAudit library itself to avoid being caught in an infinite loop.

        int index = recording.get();

        recording.set(index + 1);

        if (index == 0) {

            record(count,
                   type,
                   size < 0 ? sizeOf(obj, type) : size);

        }

        recording.set(index);

    }

    public static void record(Object obj,
                              String type) {

        if (type.charAt(0) != '[') {

            record(obj,
                   -1,
                   type,
                   -1);

        }
        else {

            long overhead = 0;

            Object[] o = (Object[])obj;

            int length = o.length;

            int count = length;

            for (int i = 1; i < type.length(); ++i) {

                if (type.charAt(i) == '[') {

                    // The following assumes the size of array of array,
                    // including the overhead of the array bookkeeping itself
                    // is only affected by the number of elements, not the
                    // actual element type.

                    overhead += sizeOf(o,
                                       "" + length + "[[L");

                    switch (type.charAt(i + 1)) {

                    case 'Z':

                        length = ((boolean[])o[0]).length;

                        break;

                    case 'B':

                        length = ((byte[])o[0]).length;

                        break;

                    case 'C':

                        length = ((char[])o[0]).length;

                        break;

                    case 'S':

                        length = ((short[])o[0]).length;

                        break;

                    case 'I':

                        length = ((int[])o[0]).length;

                        break;

                    case 'J':

                        length = ((long[])o[0]).length;

                        break;

                    case 'F':

                        length = ((float[])o[0]).length;

                        break;

                    case 'D':

                        length = ((double[])o[0]).length;

                        break;

                    case 'L':

                        length = ((Object[])o[0]).length;

                        break;

                    default:

                        o = (Object[])(o[0]);

                        length = o.length;

                        count *= length;

                    }

                }
                else {

                    record(obj,
                           count * length,
                           type.substring(i),
                           overhead + count * sizeOf(o[0],
                                                     "" + length + type.substring(i - 1)));

                    break;
                }

            }

        }

    }

    public static void record(Object obj, int count, String type) {

        record(obj,
               count,
               type,
               sizeOf(obj,
		      "" + count + "[" + type));

    }

    public static void record(Object obj,
                              int[] dimensions,
                              String type) {

        long overhead = 0;

        Object o[] = (Object[])obj;

        int count = 1;

        for (int i = 0; i < dimensions.length - 1 && count > 0; ++i) {

            int length = dimensions[i];

            if (length >= 0) {

                // The following assumes the size of array of array, including
                // the overhead of the array bookkeeping itself is only affected
                // by the number of elements, not the actual element type.

                overhead += sizeOf(o,
                                   "" + length + "[[L");

                o = (Object[])(o[0]);

            }

            count *= length;

        }

        if (count > 0) {

            int length = dimensions[dimensions.length - 1];

            record(obj,
                   count * length,
                   type,
                   overhead + count * sizeOf(o,
                                             "" + length + "[" + type));

        }

    }

    public static void record(int count,
                              String type,
                              long size) {

        try {

            for (HeapRecorder recorder: HeapRecorder.getRecorders()) {

                recorder.record(type,
                                count,
                                size);

            }

        } catch (Exception e) {

            System.err.println(e);

        }

    }

    private final static HashMap<String, HeapQuantile> recorders = new HashMap<String, HeapQuantile>();

    public static boolean inject(String id) {

        if (recorders.containsKey(id)) {

            //log("Recorder already exists for " + id);

            return false;

        }

        log("Injecting recorder for " + id);

        recorders.put(id,
                      new HeapQuantile());

        return true;

    }

    public static boolean remove(String id) {

        HeapQuantile recorder = recorders.remove(id);

        if (recorder == null) {

            
            //log("Recorder does not exist for " + id);

            return false;

        }

        log("Removing recorder for " + id);

        System.out.println(recorder.summarize(true,
                                              id));

        return true;

    }

    public static void register(String id) {

        HeapQuantile recorder = recorders.get(id);

        if (recorder != null) {

            HeapRecorder.register(recorder);

        }

    }

    public static void unregister(String id) {

        HeapQuantile recorder = recorders.get(id);

        if (recorder != null) {

            HeapRecorder.unregister(recorder);

        }

    }

}
