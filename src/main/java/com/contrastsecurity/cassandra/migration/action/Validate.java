package com.contrastsecurity.cassandra.migration.action;

import com.contrastsecurity.cassandra.migration.dao.SchemaVersionDAO;
import com.contrastsecurity.cassandra.migration.info.MigrationInfoService;
import com.contrastsecurity.cassandra.migration.info.MigrationVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.contrastsecurity.cassandra.migration.resolver.MigrationResolver;
import com.contrastsecurity.cassandra.migration.utils.StopWatch;
import com.contrastsecurity.cassandra.migration.utils.TimeFormat;

/**
 * Validates the applied migrations against the available ones.
 */
public class Validate {
	
	/**
	 * logging support
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Validate.class);

	private final SchemaVersionDAO schemaVersionDao;

	/**
	 * migration resolver
	 */
	private final MigrationResolver migrationResolver;
	
	/**
	 * migration target
	 */
	private final MigrationVersion migrationTarget;

	private final boolean outOfOrder;
	
	private final boolean pendingOrFuture;
	
	public Validate(MigrationResolver migrationResolver, SchemaVersionDAO schemaVersionDao, MigrationVersion migrationTarget, boolean outOfOrder, boolean pendingOrFuture) {
		this.schemaVersionDao = schemaVersionDao;
		this.migrationResolver = migrationResolver;
		this.migrationTarget = migrationTarget;
		this.outOfOrder = outOfOrder;
		this.pendingOrFuture = pendingOrFuture;
	}
	
	public String run() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		
		MigrationInfoService infoService = new MigrationInfoService(migrationResolver, schemaVersionDao, migrationTarget, outOfOrder, pendingOrFuture);
		infoService.refresh();
		int count = infoService.all().length;
		String validationError = infoService.validate();
		
		stopWatch.stop();
		
		LOG.info("Validated {} migrations (execution time {})", count, TimeFormat.format(stopWatch.getTotalTimeMillis()));
		
		return validationError;
	}
}
