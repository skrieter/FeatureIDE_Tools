package de.ovgu.featureide.fm.benchmark.properties;

public class Seed extends LongProperty {

	public Seed() {
		super("seed", System.currentTimeMillis());
	}

	public Seed(long defaultValue) {
		super("seed", defaultValue);
	}

}
