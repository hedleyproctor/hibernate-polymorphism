package com.hedleyproctor.domain;

import javax.persistence.Entity;

@Entity
public class Phone extends ElectricalProduct {
	private String screenSize;
	private String storage;
	public String getScreenSize() {
		return screenSize;
	}
	public void setScreenSize(String screenSize) {
		this.screenSize = screenSize;
	}
	public String getStorage() {
		return storage;
	}
	public void setStorage(String storage) {
		this.storage = storage;
	}
	
	
}
