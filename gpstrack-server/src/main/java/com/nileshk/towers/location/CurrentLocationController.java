package com.nileshk.towers.location;

import com.nileshk.towers.CacheConstants;
import org.apache.log4j.Logger;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Types;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
public class CurrentLocationController {

	private static final Logger logger = Logger.getLogger(CurrentLocationController.class);

	@Autowired
	JdbcTemplate jdbc;

	private List<Tower> nearbyTowers;

	public synchronized void setNearbyTowers(List<Tower> nearbyTowers) {
		this.nearbyTowers = nearbyTowers;
	}

	@Async
	@RequestMapping(value="/currentLocation", method = RequestMethod.POST)
	@CacheEvict(value = CacheConstants.LOCATION_LOG, allEntries = true)
	public void currentLocation(@RequestBody LocationUpdate loc) {
		logger.info("latitude: " + loc.getLatitude());
		logger.info("longitude: " + loc.getLongitude());
		storeLocation(loc.getLongitude(), loc.getLatitude());
		processLocation(loc.getLongitude(), loc.getLatitude(), loc.getDistance());
	}

	@Transactional
	private void storeLocation(Double longitude, Double latitude) {
		// TODO Get current user guid
		Integer userGid = 1;
		jdbc.update("INSERT INTO location_log (user_gid, the_geom) VALUES\n"
				+ "  (?, ST_SetSRID(ST_MakePoint(?, ?), 4326))", userGid, longitude, latitude);
	}

	@Transactional
	private void processLocation(Double longitude, Double latitude, Double searchDistance) {
		logger.info("Processing location...");

		if (searchDistance == null) {
			searchDistance = 0.6;
		}
		logger.info("Search distance = " + searchDistance);
		List<Map<String, Object>> results = jdbc.queryForList("SELECT\n"
						+ "  *,\n"
						+ "  ST_Distance(the_geom,\n"
						+ "              ST_SetSRID(ST_MakePoint(?, ?), 4326)\n"
						+ "  ) AS distance\n"
						+ "FROM towers\n"
						+ "WHERE ST_DWithin(the_geom, ST_SetSRID(ST_MakePoint(?, ?), 4326), ? / 111.325)\n"
						+ "ORDER BY distance",
				longitude, latitude, longitude, latitude, searchDistance);
		List<Tower> towers = results.stream().map(result -> {
			Integer gid = (Integer) result.get("gid");
			String name = (String) result.get("name");
			PGgeometry geo = (PGgeometry) result.get("the_geom");
			Point point = geo.getGeometry().getFirstPoint();
			Double distance = (Double) result.get("distance");
			logger.info("[" + point.y + ", " + point.x + "] " + distance + " " + name);
			Tower tower = new Tower();
			tower.setGid(gid);
			tower.setName(name);
			LongLat longLat = new LongLat();
			longLat.setLongitude(point.x);
			longLat.setLatitude(point.y);
			tower.setLongLat(longLat);
			tower.setDistance(distance);
			return tower;
		}).collect(toList());
		setNearbyTowers(towers);
		updateStatus(towers);
	}

	private void updateStatus(List<Tower> towers) {
		/*
		TODO Only increase strength if distance has changed past a certain threshold
		  */
		// Increment strength if less than 100
		List<Object[]> ids = towers.stream().map(t -> new Object[] { t.getGid() }).collect(toList());
		jdbc.batchUpdate("UPDATE towers\n"
				+ "SET strength = strength + 1,\n"
				+ "  strength_timestamp = now()\n"
				+ "WHERE strength < 100\n"
				+ "      AND strength_timestamp < now() - INTERVAL '1 minute'\n"
				+ "      AND gid = ?\n", ids, new int[] { Types.INTEGER });
	}

	@RequestMapping(value="/nearby", method = RequestMethod.POST)
	public List<Tower> nearbyTowers() {
		return nearbyTowers;
	}

}
