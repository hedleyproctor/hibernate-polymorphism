package com.hedleyproctor.domain;

import javax.persistence.Entity;

@Entity
public class RingProduct extends Product {
	private String stoneType;
	private String stoneSize;
	
	public String getStoneType() {
		return stoneType;
	}
	public void setStoneType(String stoneType) {
		this.stoneType = stoneType;
	}
	public String getStoneSize() {
		return stoneSize;
	}
	public void setStoneSize(String stoneSize) {
		this.stoneSize = stoneSize;
	}
	
	
}
