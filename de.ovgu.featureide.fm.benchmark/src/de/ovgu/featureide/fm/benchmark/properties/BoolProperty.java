package de.ovgu.featureide.fm.benchmark.properties;

public class BoolProperty extends AProperty<Boolean> {

	public BoolProperty(String name) {
		super(name, Boolean.FALSE);
	}

	public BoolProperty(String name, Boolean defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected Boolean cast(String valueString) throws Exception {
		return Boolean.parseBoolean(valueString);
	}

}
