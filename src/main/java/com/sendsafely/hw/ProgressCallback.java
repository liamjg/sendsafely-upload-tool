package com.sendsafely.hw;


import java.text.MessageFormat;

import com.sendsafely.ProgressInterface;

public class ProgressCallback implements ProgressInterface {
	
	private double progress = 0;

	@Override
	public void updateProgress(String fileId, double progress) {
		// TODO: make this formatting better
		if(progress > this.progress) {
			this.progress = progress;
			System.out.println("  Progress: " + MessageFormat.format("{0,number,#.##%}", progress));
		}
	}

	@Override
	public void gotFileId(String fileId) {
		System.out.println("  File Id: " + fileId);	
	}
}
