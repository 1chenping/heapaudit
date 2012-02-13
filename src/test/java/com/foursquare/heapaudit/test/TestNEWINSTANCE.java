package com.foursquare.heapaudit.test;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestNEWINSTANCE extends TestUtil {

    // Test allocation of objects via new-instance.

    @Test public void NEWINSTANCE_1d() {

	clear();

	Object arrayL = Array.newInstance(TestChild.class,
					  3);

	assertTrue(expect("com.foursquare.heapaudit.test.TestChild",
			  3,
			  32));

	assertTrue(empty());

    }

    @Test public void NEWINSTANCE_2d() {

	clear();

	Object arrayL = Array.newInstance(TestChild.class,
					  5,
					  7);

	assertTrue(expect("com.foursquare.heapaudit.test.TestChild",
			  35,
			  280));

	assertTrue(expect("I",
			  2,
			  24));

	assertTrue(empty());

    }

    @Test public void NEWINSTANCE_L0() throws InstantiationException, IllegalAccessException {

	clear();

	Object objectL = TestChild.class.newInstance();

	assertTrue(expect("com.foursquare.heapaudit.test.TestChild",
			  -1,
			  16));

	assertTrue(empty());

    }

    @Test public void NEWINSTANCE_Lx() throws InstantiationException, IllegalAccessException , IllegalArgumentException, InvocationTargetException {

	clear();

	Object objectL = TestParent.class.getDeclaredConstructors()[0].newInstance(99);

	assertTrue(expect("com.foursquare.heapaudit.test.TestParent",
			  -1,
			  16));

	assertTrue(expect("java.lang.Object",
			  1,
			  24));

	assertTrue(empty());

    }

}
