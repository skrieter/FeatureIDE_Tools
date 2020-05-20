package de.ovgu.featureide.fm.benchmark.properties;

public class LongProperty extends AProperty<Long> {

	public LongProperty(String name) {
		super(name, 0L);
	}

	public LongProperty(String name, Long defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected Long cast(String valueString) throws Exception {
		return Long.parseLong(valueString);
	}

}
