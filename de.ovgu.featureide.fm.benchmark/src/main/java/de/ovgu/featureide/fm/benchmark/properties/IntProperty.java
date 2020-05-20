package de.ovgu.featureide.fm.benchmark.properties;

public class IntProperty extends AProperty<Integer> {

	public IntProperty(String name) {
		super(name, 0);
	}

	public IntProperty(String name, int defaultValue) {
		super(name, defaultValue);
	}

	@Override
	protected Integer cast(String valueString) throws Exception {
		return Integer.parseInt(valueString);
	}

}
