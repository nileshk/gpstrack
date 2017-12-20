package com.nileshk.towers.location;

import com.nileshk.towers.CacheConstants;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocationHistoryController {

	private static final Logger logger = Logger.getLogger(LocationHistoryController.class);

	@Autowired
	JdbcTemplate jdbc;

	@RequestMapping(value = "/history", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@Transactional(readOnly = true)
	@Cacheable(CacheConstants.LOCATION_LOG)
	public String historyFeatureCollection() {
		String sql = "SELECT row_to_json(fc)\n"
				+ "  FROM ( SELECT 'FeatureCollection' AS type, array_to_json(array_agg(f)) AS features\n"
				+ "           FROM ( SELECT 'Feature' AS type,\n"
				+ "                         ST_AsGeoJSON(lg.the_geom)::JSON AS geometry,\n"
				+ "                         row_to_json((SELECT l FROM (SELECT gid, user_gid, submit_timestamp, EXTRACT(EPOCH FROM (submit_timestamp AT TIME ZONE 'UTC')) * 1000 AS submit_time) AS l)) AS properties\n"
				+ "                  FROM (\n"
				+ "                         SELECT * FROM location_log \n"
				+ "                         ORDER BY submit_timestamp DESC \n"
				//+ "                         LIMIT 10000 \n"
				+ "                    ) lg ) AS f )  AS fc\n";
		logger.info("Starting feature collection query...");
		String featureCollection = jdbc.queryForObject(sql, String.class);
		logger.info("Feature collection fetched, returning...");
		return featureCollection;
	}

}
