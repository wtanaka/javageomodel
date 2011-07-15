package com.beoui.utils;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.beoui.geocell.annotations.Geocells;
import com.beoui.geocell.annotations.Latitude;
import com.beoui.geocell.annotations.Longitude;

@Entity
public class JPAEntity {

	@Id
	String id;

	@Longitude
	double longitude;

	@Latitude
	double latitude;

	@Geocells
	@OneToMany(fetch = FetchType.EAGER)
    private List<String> geoCellsData = new ArrayList<String>();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public List<String> getGeoCellsData() {
    	return geoCellsData;
    }

	public void setGeoCellsData(List<String> geocells) {
    	this.geoCellsData = geocells;
    }

}
