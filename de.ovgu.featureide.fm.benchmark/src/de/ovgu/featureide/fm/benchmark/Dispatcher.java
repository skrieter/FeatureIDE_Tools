package de.ovgu.featureide.fm.benchmark;

import java.lang.reflect.Method;
import java.util.Arrays;

public class Dispatcher {

	public static void main(final String[] args) throws Exception {
		if (args.length < 1) {
			throw new IndexOutOfBoundsException("The first argument must be the name of the main class!");
		}

		final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		final Class<?> loadClass = systemClassLoader.loadClass(args[0]);
		final Method method = loadClass.getMethod("main", String[].class);

		method.invoke(null, new Object[] { Arrays.copyOfRange(args, 1, args.length) });
	}

}