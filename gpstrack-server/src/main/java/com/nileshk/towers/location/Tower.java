package com.nileshk.towers.location;

public class Tower {

	private Integer gid;
	private String name;
	private LongLat longLat;
	private Double distance;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LongLat getLongLat() {
		return longLat;
	}

	public void setLongLat(LongLat longLat) {
		this.longLat = longLat;
	}

	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public Integer getGid() {
		return gid;
	}

	public void setGid(Integer gid) {
		this.gid = gid;
	}
}
