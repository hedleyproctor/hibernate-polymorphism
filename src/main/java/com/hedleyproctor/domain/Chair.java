package com.hedleyproctor.domain;

import javax.persistence.Entity;

@Entity
public class Chair extends FurnitureProduct {
	private String material;

	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}
	
	

}
