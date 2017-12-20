package com.nileshk.towers;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InitController {

	private static final Logger logger = Logger.getLogger(InitController.class);

	@Autowired
	JdbcTemplate jdbc;

	@RequestMapping(value = "/init", method = RequestMethod.POST)
	public void initAllTables() {
		doCreateTable("users", this::initUsers);
		doCreateTable("location_log", this::initLocationLog);
		doCreateTable("towers", this::initTowers);
	}

	@RequestMapping(value = "/initUsers", method = RequestMethod.POST)
	@Transactional
	public void initUsers() {
		String sql = "CREATE TABLE users  \n"
				+ "        (\n"
				+ "          gid SERIAL NOT NULL,\n"
				+ "          name VARCHAR(16),\n"
				+ "          submit_timestamp TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp, \n"
				+ "          CONSTRAINT users_pkey PRIMARY KEY (gid),\n"
				+ "          CONSTRAINT users_name_unique UNIQUE (name)\n"
				+ "        )\n"
				+ "        WITH (\n"
				+ "          OIDS=FALSE\n"
				+ "        )\n"
				+ "  ";
		jdbc.execute(sql);
	}

	@RequestMapping(value = "/initLocationLog", method = RequestMethod.POST)
	@Transactional
	public void initLocationLog() {
		String sql = "CREATE TABLE location_log\n"
				+ "        (\n"
				+ "          gid SERIAL NOT NULL,\n"
				+ "          user_gid INTEGER REFERENCES users(gid),\n"
				+ "          the_geom GEOMETRY,\n"
				+ "          submit_timestamp TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,\n"
				+ "          CONSTRAINT llog_pkey PRIMARY KEY (gid),\n"
				+ "          CONSTRAINT llog_enforce_dims_geom CHECK (st_ndims(the_geom) = 2),\n"
				+ "          CONSTRAINT llog_enforce_geotype_geom CHECK (geometrytype(the_geom) = 'POINT'::TEXT OR the_geom IS NULL),\n"
				+ "          CONSTRAINT llog_enforce_srid_geom CHECK (st_srid(the_geom) = 4326)\n"
				+ "        )\n"
				+ "        WITH (\n"
				+ "          OIDS=FALSE\n"
				+ "        )\n";
		jdbc.execute(sql);
	}

	@RequestMapping(value = "/initTowers", method = RequestMethod.POST)
	@Transactional
	public void initTowers() {
		String sql = "CREATE TABLE towers\n"
				+ "        (\n"
				+ "          gid SERIAL NOT NULL,\n"
				+ "          name TEXT,\n"
				+ "          owner_gid INTEGER REFERENCES users(gid),\n"
				+ "          strength SMALLINT,\n"
				+ "          the_geom GEOMETRY,\n"
				+ "          submit_timestamp TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,\n"
				+ "          strength_timestamp TIMESTAMP WITH TIME ZONE DEFAULT current_timestamp,\n"
				+ "          CONSTRAINT towers_pkey PRIMARY KEY (gid),\n"
				+ "          CONSTRAINT prtl_enforce_dims_geom CHECK (st_ndims(the_geom) = 2),\n"
				+ "          CONSTRAINT prtl_enforce_geotype_geom CHECK (geometrytype(the_geom) = 'POINT'::TEXT OR the_geom IS NULL),\n"
				+ "          CONSTRAINT prtl_enforce_srid_geom CHECK (st_srid(the_geom) = 4326)\n"
				+ "        )\n"
				+ "        WITH (\n"
				+ "          OIDS=FALSE\n"
				+ "        )\n";
		jdbc.execute(sql);
		}

	private void doCreateTable(String tableName, Runnable runnable) {
		try {
			logger.info(("Attempting to create table " + tableName));
			runnable.run();
			logger.info("Created '" + tableName + "' table");
		} catch (Exception ex) {
			logger.error("Exception initializing " + tableName + " table", ex);
		}
	}

}
