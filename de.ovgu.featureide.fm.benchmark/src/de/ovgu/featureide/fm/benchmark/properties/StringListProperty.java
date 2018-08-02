package de.ovgu.featureide.fm.benchmark.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringListProperty extends AProperty<List<String>> {

	public StringListProperty(String name) {
		super(name);
	}

	@Override
	protected List<String> getDefaultValue() {
		return Collections.emptyList();
	}

	@Override
	protected List<String> cast(String valueString) throws Exception {
		return Arrays.asList(valueString.split(","));
	}

}
