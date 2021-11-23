package com.sendsafely.hw;


public class PackageOperation {

	private Operation operation;
	private String payload;

	public PackageOperation(Operation operation, String payload) {
		this.operation = operation;
		this.payload = payload;

	}

	public Operation getOperation() {
		return operation;
	}

	public String getPayload() {
		return payload;
	}
}
