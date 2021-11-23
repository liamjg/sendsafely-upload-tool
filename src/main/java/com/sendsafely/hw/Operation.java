package com.sendsafely.hw;


public enum Operation {
	ADD_RECIPIENT("Add recipient"), ADD_FILE("Add file");

	private String description;

	private Operation(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}
}
