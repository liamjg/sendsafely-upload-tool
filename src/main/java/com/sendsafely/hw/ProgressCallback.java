package com.sendsafely.hw;

import java.text.MessageFormat;

import com.sendsafely.ProgressInterface;

public class ProgressCallback implements ProgressInterface {

	private double progress = 0;

	@Override
	public void updateProgress(String fileId, double progress) {
		// only print the progress if it is 'new'
		if (progress > this.progress) {
			this.progress = progress;
			System.out.println(MessageFormat.format("  Progess: {0,number,#.##%}", progress));
		}
	}

	@Override
	public void gotFileId(String fileId) {
		System.out.println("  File Id: " + fileId);
	}
}
