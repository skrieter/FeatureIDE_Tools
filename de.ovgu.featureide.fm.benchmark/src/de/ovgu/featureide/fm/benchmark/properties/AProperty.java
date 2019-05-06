package de.ovgu.featureide.fm.benchmark.properties;

import de.ovgu.featureide.fm.benchmark.ABenchmark;

public abstract class AProperty<T> implements IProperty {

	private final String key;
	private final T defaultValue;
	private T value;

	public AProperty(String key) {
		this(key, null);
	}

	public AProperty(String key, T defaultValue) {
		this.key = key;
		this.defaultValue = defaultValue;
		ABenchmark.addProperty(this);
	}

	public T getValue() {
		return value;
	}

	public String getKey() {
		return key;
	}

	protected T getDefaultValue() {
		return defaultValue;
	}

	protected abstract T cast(String valueString) throws Exception;

	public boolean setValue(String valueString) {
		if (valueString != null) {
			try {
				value = cast(valueString);
				return true;
			} catch (Exception e) {
			}
		}
		value = getDefaultValue();
		return false;
	}

	@Override
	public String toString() {
		return value != null ? value.toString() : "no value";
	}

}
