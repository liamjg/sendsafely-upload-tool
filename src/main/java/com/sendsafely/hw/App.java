package com.sendsafely.hw;

import java.util.Arrays;

import com.sendsafely.exceptions.InvalidCredentialsException;

public class App {

	private static final String[] DEBUG_FLAGS = { "-d", "--debug" };

	public static void main(String... args) {

		if (args == null || args.length < 3 || args.length > 4) {
			// Invalid number of arguments. Print the usage syntax to the screen and exit.
			System.out.println("Usage: SendSafelyHost UserApiKey UserApiSecret -d:--debug");
			return;
		}

		String host = args[0];
		String key = args[1];
		String secret = args[2];
		boolean debug = args.length == 4 && Arrays.asList(DEBUG_FLAGS).contains(args[3]);

		try {
			new SendSafelyUploadTool(host, key, secret, debug).runTool();
		} catch (InvalidCredentialsException e) {
			System.out.println("Invalid credentials.");
			if (debug)
				e.printStackTrace();
		}
	}
}
