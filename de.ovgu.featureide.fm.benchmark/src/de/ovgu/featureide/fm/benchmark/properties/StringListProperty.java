package de.ovgu.featureide.fm.benchmark.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringListProperty extends AProperty<List<String>> {

	public StringListProperty(String name) {
		super(name, Collections.emptyList());
	}

	public StringListProperty(String name, List<String> defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected List<String> cast(String valueString) throws Exception {
		return Arrays.asList(valueString.split(","));
	}

}
