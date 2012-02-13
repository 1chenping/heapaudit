package com.foursquare.heapaudit.test;

import com.foursquare.heapaudit.HeapRecorder;
import java.util.concurrent.ConcurrentLinkedQueue;

@HeapRecorder.Suppress public class TestUtil {

    public TestUtil() {

	this(false);

    }

    public TestUtil(boolean global) {

	HeapRecorder.register(recorder,
			      global);

    }

    public void clear() {

	recorder.clear();

    }

    public boolean expect(String name,
			  int count,
			  long size) {

	return recorder.expect(name,
			       count,
			       size);

    }

    public boolean empty() {

	return recorder.empty();

    }

    private final Recorder recorder = new Recorder();

    private class Recorder extends HeapRecorder {

	@Override public void record(String name,
				     int count,
				     long size) {

	    entries.add(new Entry(friendly(name),
				  count,
				  size));

	}

	public void clear() {

	    entries.clear();

	}

	public boolean expect(String name,
			      int count,
			      long size) {

	    return entries.remove(new Entry(friendly(name),
					    count,
					    size));

	}

	public boolean empty() {

	    if (entries.isEmpty()) {

		return true;

	    }

	    System.out.println(entries);

	    return false;

	}

	private class Entry {

	    public Entry(String name,
			 int count,
			 long size) {

		this.name = name;

		this.count = count;

		this.size = size;

	    }

	    @Override public boolean equals(Object obj) {

		Entry e = (Entry)obj;

		return name.equals(e.name) && (count == e.count) && (size == e.size);

	    }

            @Override public String toString() {

		return name + "[" + count + "] " + size;

	    }

	    public final String name;

	    public final int count;

	    public final long size;

	}

	private final ConcurrentLinkedQueue<Entry> entries = new ConcurrentLinkedQueue<Entry>();

    }

}
