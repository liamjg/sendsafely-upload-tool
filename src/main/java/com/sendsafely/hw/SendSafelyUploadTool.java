package com.sendsafely.hw;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.sendsafely.Package;
import com.sendsafely.Recipient;
import com.sendsafely.SendSafely;
import com.sendsafely.dto.PackageURL;
import com.sendsafely.exceptions.ApproverRequiredException;
import com.sendsafely.exceptions.CreatePackageFailedException;
import com.sendsafely.exceptions.FinalizePackageFailedException;
import com.sendsafely.exceptions.InvalidCredentialsException;
import com.sendsafely.exceptions.LimitExceededException;
import com.sendsafely.exceptions.RecipientFailedException;
import com.sendsafely.exceptions.UploadFileException;
import com.sendsafely.file.DefaultFileManager;
import com.sendsafely.file.FileManager;

public class SendSafelyUploadTool {
	private static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern
			.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

	private boolean debug = false;

	private SendSafely sendSafelyCli = null;
	private Deque<PackageOperation> operations = new ArrayDeque<PackageOperation>();

	private Scanner in = null;

	public SendSafelyUploadTool(String host, String key, String secret, boolean debug)
			throws InvalidCredentialsException {
		this.sendSafelyCli = getSendSafelyCli(host, key, secret);
		this.debug = debug;
	}

	// main loop
	public void runTool() {
		this.in = new Scanner(System.in);

		boolean run = true;

		do {
			System.out.println("\nWhat would you like to do?");
			System.out.println("1)	Create a new package");
			System.out.println("0)	Exit");
			System.out.print("\nChoose (01): ");

			String input = this.in.nextLine();

			switch (input) {
			case "1":
				runCreateAPackage();
				break;
			case "0":
				run = false;
				break;
			default:
				System.out.println("ERROR: Invalid input");
			}
		} while (run);

		this.in.close();
		System.out.println("\nDone.");
	}

	// create a package loop
	private void runCreateAPackage() {
		Package pkg = createPackage();

		if (pkg == null)
			return;

		String packageId = pkg.getPackageId();
		System.out.println("Created new package with ID " + packageId + ".");

		boolean run = true;

		do {
			// TODO: better way to parse these out?
			List<String> recipients = new ArrayList<String>();
			List<String> paths = new ArrayList<String>();

			operations.forEach(operation -> {
				if (operation.getOperation() == Operation.ADD_FILE) {
					paths.add(operation.getPayload());

				} else if (operation.getOperation() == Operation.ADD_RECIPIENT) {
					recipients.add(operation.getPayload());
				}
			});

			printPackageData(packageId, recipients, paths);

			System.out.println("\nWhat would you like to do?");
			System.out.println("1)	Add recipient(s)");
			System.out.println("2)	Add file(s)");
			System.out.println("3)	Finalize package");
			System.out.println("u)	Undo");
			System.out.println("0)	Cancel");
			System.out.print("\nChoose (0123u): ");

			String input = this.in.nextLine();

			switch (input) {
			case "1":
				addRecipient();
				break;
			case "2":
				addFile();
				break;
			case "3":
				PackageURL result = finalizePackage(pkg, recipients, paths);

				if (result != null) {
					System.out.println("\nFinalized Package:");
					System.out.println(result.getSecureLink());
					run = false;
				}

				break;
			case "u":
				undoOperation();
				break;
			case "0":
				run = false;
				break;
			default:
				System.out.println("ERROR: Invalid input");
			}
		} while (run);
	}

	private Package createPackage() {
		System.out.println("\nCreating a new package...");
		Package pkgInfo = null;

		try {
			pkgInfo = this.sendSafelyCli.createPackage();
		} catch (CreatePackageFailedException e) {
			System.out.println("\nERROR: Package creation failed");
			if (this.debug) {
				e.printStackTrace();
			}

		} catch (LimitExceededException e) {
			System.out.println("\nERROR: Package limit exceeded");
			if (this.debug) {
				e.printStackTrace();
			}
		}
		return pkgInfo;
	}

	private void printPackageData(String packageId, List<String> recipients, List<String> paths) {
		System.out.println("\nPackage");
		System.out.println("  " + packageId);

		System.out.println("Recipients");
		if (recipients.size() > 0) {
			recipients.forEach(recipient -> System.out.println("  " + recipient));
		} else {
			System.out.println("  (none)");
		}

		System.out.println("Files");
		if (paths.size() > 0) {
			paths.forEach(file -> System.out.println("  " + file));
		} else {
			System.out.println("  (none)");
		}
	}

	private boolean operationExists(Operation operation, String payload) {
		return operations.stream().anyMatch(existingOperation -> (existingOperation.getOperation().equals(operation)
				&& existingOperation.getPayload().equals(payload)));
	}

	private void addRecipient() {
		System.out.print("\nEnter recipient email: ");
		String recipient = this.in.nextLine();

		if (VALID_EMAIL_ADDRESS_REGEX.matcher(recipient).find()) {
			if (operationExists(Operation.ADD_RECIPIENT, recipient)) {
				System.out.println("\nERROR: Email already exists");
			} else {
				operations.push(new PackageOperation(Operation.ADD_RECIPIENT, recipient));
				System.out.println("Added " + recipient + " as recipient");
			}
		} else {
			System.out.println("\nERROR: Invalid email address");
		}

	}

	private void addFile() {
		System.out.println("\nEnter path to file: ");
		String path = this.in.nextLine();

		if (new java.io.File(path).exists()) {
			if (operationExists(Operation.ADD_FILE, path)) {
				System.out.println("\nERROR: File already exists");
			} else {
				operations.push(new PackageOperation(Operation.ADD_FILE, path));
				System.out.println("Added " + path);
			}
		} else {
			System.out.println("\nERROR: File does not exist");
		}
	}

	private PackageURL finalizePackage(Package pkg, List<String> recipients, List<String> paths) {
		if (recipients.size() == 0 || paths.size() == 0) {
			System.out.println("\nERROR: Package must have at least 1 file and at least 1 recipient to finalize");
			return null;
		}

		String packageId = pkg.getPackageId();
		String keyCode = pkg.getKeyCode();

		System.out.println("\nFinalizing Package...");

		System.out.println("\nAdding Recipients...");
		for (String recipient : recipients) {
			Recipient newRecipient = null;
			try {
				newRecipient = this.sendSafelyCli.addRecipient(packageId, recipient);
			} catch (LimitExceededException e) {
				System.out.println("\nERROR: Recipient limit exceeded");
				if (this.debug)
					e.printStackTrace();
			} catch (RecipientFailedException e) {
				System.out.println("\nERROR: Failed to add recipient");
				if (this.debug)
					e.printStackTrace();
			}
			System.out.println("Added " + newRecipient.getEmail() + " as recipient");
		}

		System.out.println("\nAdding Files...");
		for (String file : paths) {
			FileManager uploadManager = null;
			try {
				uploadManager = new DefaultFileManager(new java.io.File(file));
			} catch (IOException e) {
				System.out.println("\nERROR: Failed to initate SendSafely upload manager");
				if (this.debug)
					e.printStackTrace();
			}

			System.out.println("Uploading " + file + " ...");

			try {
				this.sendSafelyCli.encryptAndUploadFile(packageId, keyCode, uploadManager, new ProgressCallback());
			} catch (LimitExceededException e) {
				System.out.println("\nERROR: File limit exceeded");
				if (this.debug)
					e.printStackTrace();
			} catch (UploadFileException e) {
				System.out.println("\nERROR: Failed to upload file");
				if (this.debug)
					e.printStackTrace();
			}
			System.out.println("Upload Complete.");
		}

		PackageURL packageLink = null;
		try {
			packageLink = this.sendSafelyCli.finalizePackage(packageId, keyCode);
		} catch (LimitExceededException e) {
			System.out.println("\nERROR: Package limit exceeded");
			if (this.debug)
				e.printStackTrace();
		} catch (FinalizePackageFailedException e) {
			System.out.println("\nERROR: Failed to finalize package");
			if (this.debug)
				e.printStackTrace();
		} catch (ApproverRequiredException e) {
			System.out.println("\nERROR: Missing required approver");
			if (this.debug)
				e.printStackTrace();
		}

		return packageLink;
	}

	private void undoOperation() {
		if (operations.size() == 0) {
			System.out.println("\nERROR: No operations to undo");
			return;
		}

		PackageOperation undoneOperation = operations.pop();
		System.out.println(
				"Undid Operation " + undoneOperation.getOperation().toString() + " : " + undoneOperation.getPayload());
	}

	private SendSafely getSendSafelyCli(String host, String key, String secret) throws InvalidCredentialsException {
		System.out.println("Authenticating... ");
		SendSafely sendSafelyCli = new SendSafely(host, key, secret);

		String loggedInUser = sendSafelyCli.verifyCredentials();
		System.out.println("Connected as " + loggedInUser);

		return sendSafelyCli;
	}

}
