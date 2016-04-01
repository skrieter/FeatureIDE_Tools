package de.ovgu.featureide.fm.benchmark.properties;

public abstract class AProperty<T> implements IProperty {

	private final String key;
	private T value;

	public AProperty(String key) {
		this.key = key;
	}

	public T getValue() {
		return value;
	}

	public String getKey() {
		return key;
	}

	protected abstract T getDefaultValue();

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
		return value != null ? value.toString() : "null";
	}

}
