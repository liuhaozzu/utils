/**
 * 
 */
package com.liuhaozzu.utils.packagescan;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import sun.misc.ClassLoaderUtil;

/**
 * @author Administrator
 *
 */
public class JarScanner {
	private static final String jarFilePath = "E:\\workspaces\\sts-3.8.3\\java8\\target\\java8-0.0.1-SNAPSHOT.jar";
	private static final String jarFilePathURL = "file:E:\\workspaces\\sts-3.8.3\\java8\\target\\java8-0.0.1-SNAPSHOT.jar";

	public static void main(String[] args) throws Exception {
		Map<String, List<String>> testCaseMap = new ConcurrentHashMap<>();
		// scanPkg();
		// System.out.println(new URL(jarFilePath));

		URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { new URL(jarFilePathURL) },
				Thread.currentThread().getContextClassLoader());

		JarFile jarFile = new JarFile(jarFilePath);

		List<JarEntry> list = jarFile.stream().parallel().filter(jarEntry -> jarEntry.toString().endsWith(".class"))
				.collect(Collectors.toList());
		list.parallelStream().forEach(item -> {
			String clazzName = item.toString().replace(".class", "").replace('/', '.');
			Class<?> clazz = null;
			try {
				clazz = urlClassLoader.loadClass(clazzName);
			} catch (ClassNotFoundException e) {
				// do nothing
			}
			if (clazz != null) {
				final List<String> testcaseList;
				if (testCaseMap.containsKey(clazzName)) {
					testcaseList = testCaseMap.get(clazzName);

				} else {
					testcaseList = new ArrayList<>();
					testCaseMap.put(clazzName, testcaseList);
				}
				if (clazz.isAnnotationPresent(Test.class)) {
					Method[] methods = clazz.getDeclaredMethods();
					Stream.of(methods).forEach(method -> testcaseList.add(method.getName()));
				} else {
					Method[] methods = clazz.getMethods();
					Stream.of(methods).forEach(method -> {
						if (method.isAnnotationPresent(Test.class)) {
							testcaseList.add(method.getName());
						}
					});
				}

			}
		});
		ClassLoaderUtil.releaseLoader(urlClassLoader);
		testCaseMap.forEach((key, valueList) -> valueList.stream()
				.forEach(value -> System.out.println(value + "::" + key.substring(key.lastIndexOf('.') + 1))));
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File("testCaseMap.json"), testCaseMap);
	}
}
