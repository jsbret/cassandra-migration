package com.contrastsecurity.cassandra.migration;

import com.contrastsecurity.cassandra.migration.config.Keyspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CommandLine {

	/**
	 * command to trigger migrate action
	 */
	public static final String MIGRATE = "migrate";

	/**
	 * command to trigger validate action
	 */
	public static final String VALIDATE = "validate";

	/**
	 * logging support
	 */
	private static Logger LOG;

	/**
	 * @param args
	 *            command line arguments
	 */
	public static void main(String[] args) {
		initLogging();

		List<String> operations = determineOperations(args);
		if (operations.isEmpty()) {
			printUsage();
			return;
		}

		String operation = operations.get(0);

		CassandraMigration cm = new CassandraMigration();
		Keyspace ks = new Keyspace();
		cm.setKeyspace(ks);
		if (MIGRATE.equalsIgnoreCase(operation)) {
			cm.migrate();
		} else if (VALIDATE.equalsIgnoreCase(operation)) {
			cm.validate();
		}
	}

	private static List<String> determineOperations(String[] args) {
		List<String> operations = new ArrayList<>();

		for (String arg : args) {
			if (!arg.startsWith("-")) {
				operations.add(arg);
			}
		}

		return operations;
	}

	static void initLogging() {
		LOG = LoggerFactory.getLogger(CommandLine.class);
	}

	private static void printUsage() {
		LOG.info("********");
		LOG.info("* Usage");
		LOG.info("********");
		LOG.info("");
		LOG.info("cassandra-migration [options] command");
		LOG.info("");
		LOG.info("Commands");
		LOG.info("========");
		LOG.info("migrate  : Migrates the database");
		LOG.info("validate : Validates the applied migrations against the available ones");
		LOG.info("");
		LOG.info("Add -X to print debug output");
		LOG.info("Add -q to suppress all output, except for errors and warnings");
		LOG.info("");
	}
}
