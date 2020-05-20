package de.ovgu.featureide.fm.benchmark.properties;

public class StringProperty extends AProperty<String> {

	public StringProperty(String name) {
		super(name, "");
	}

	public StringProperty(String name, String defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected String cast(String valueString) throws Exception {
		return valueString;
	}

}
