package de.ovgu.featureide.fm.benchmark.properties;

import de.ovgu.featureide.fm.benchmark.BenchmarkConfig;

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
		BenchmarkConfig.addProperty(this);
	}

	public T getValue() {
		return (value != null) ? value : defaultValue;
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
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(" = ");
		if (value != null) {
			 sb.append(value.toString());
		} else if (defaultValue != null) {
			 sb.append(defaultValue.toString());
			 sb.append(" (default value)");
		} else {
			 sb.append("null");
		}
		return sb.toString();
	}

}
