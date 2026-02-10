package com.contrastsecurity.cassandra.migration;

import com.contrastsecurity.cassandra.migration.config.MigrationType;
import com.contrastsecurity.cassandra.migration.info.MigrationInfo;
import com.contrastsecurity.cassandra.migration.info.MigrationInfoDumper;
import com.contrastsecurity.cassandra.migration.info.MigrationInfoService;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class CassandraMigrationIT extends BaseIT {

	@Test
	public void runApiTest() {
		String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
		CassandraMigration cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(scriptsLocations);
		cm.setKeyspace(getKeyspace());
		cm.migrate();

		MigrationInfoService infoService = cm.info();
		System.out.println("Initial migration");
		System.out.println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()));
		assertThat(infoService.all()).hasSize(4);
		for (MigrationInfo info : infoService.all()) {
			assertThat(info.getVersion().getVersion()).isIn("1.0.0", "2.0.0", "3.0", "3.0.1");
			if (info.getVersion().equals("3.0.1")) {
				assertThat(info.getDescription()).isEqualTo("Three point zero one");
				assertThat(info.getType().name()).isEqualTo(MigrationType.JAVA_DRIVER.name());
				assertThat(info.getScript()).contains(".java");

				ResultSet result = getSession().execute("SELECT value FROM test1 WHERE space = 'web' AND key = 'facebook'");
				assertThat(result.one().getString("value")).isEqualTo("facebook.com");
			} else if (info.getVersion().equals("3.0")) {
				assertThat(info.getDescription()).isEqualTo("Third");
				assertThat(info.getType().name()).isEqualTo(MigrationType.JAVA_DRIVER.name());
				assertThat(info.getScript()).contains(".java");

				ResultSet result = getSession().execute("SELECT value FROM test1 WHERE space = 'web' AND key = 'google'");
				assertThat(result.one().getString("value")).isEqualTo("google.com");
			} else if (info.getVersion().equals("2.0.0")) {
				assertThat(info.getDescription()).isEqualTo("Second");
				assertThat(info.getType().name()).isEqualTo(MigrationType.CQL.name());
				assertThat(info.getScript()).contains(".cql");

				Row row = getSession().execute("SELECT title, message FROM contents WHERE id = 1").one();
				assertThat(row.getString("title")).isEqualTo("foo");
				assertThat(row.getString("message")).isEqualTo("bar");
			} else if (info.getVersion().equals("1.0.0")) {
				assertThat(info.getDescription()).isEqualTo("First");
				assertThat(info.getType().name()).isEqualTo(MigrationType.CQL.name());
				assertThat(info.getScript()).contains(".cql");

				ResultSet result = getSession().execute("SELECT value FROM test1 WHERE space = 'foo' AND key = 'bar'");
				assertThat(result.one().getString("value")).isEqualTo("profit!");
			}

			assertThat(info.getState().isApplied()).isTrue();
			assertThat(info.getInstalledOn()).isNotNull();
		}

		// test out of order when out of order is not allowed
		String[] outOfOrderScriptsLocations = { "migration/integ_outoforder", "migration/integ/java" };
		cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(outOfOrderScriptsLocations);
		cm.setKeyspace(getKeyspace());
		cm.migrate();

		infoService = cm.info();
		System.out.println("Out of order migration with out-of-order ignored");
		System.out.println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()));
		assertThat(infoService.all()).hasSize(5);
		for (MigrationInfo info : infoService.all()) {
			assertThat(info.getVersion().getVersion())
					.isIn("1.0.0", "2.0.0", "3.0", "3.0.1", "1.1.1");
			if (info.getVersion().equals("1.1.1")) {
				assertThat(info.getDescription()).isEqualTo("Late arrival");
				assertThat(info.getType().name()).isEqualTo(MigrationType.CQL.name());
				assertThat(info.getScript()).contains(".cql");
				assertThat(info.getState().isApplied()).isFalse();
				assertThat(info.getInstalledOn()).isNull();
			}
		}

		// test out of order when out of order is allowed
		String[] outOfOrder2ScriptsLocations = { "migration/integ_outoforder2", "migration/integ/java" };
		cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(outOfOrder2ScriptsLocations);
		cm.getConfigs().setAllowOutOfOrder(true);
		cm.setKeyspace(getKeyspace());
		cm.migrate();

		infoService = cm.info();
		System.out.println("Out of order migration with out-of-order allowed");
		System.out.println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()));
		assertThat(infoService.all()).hasSize(6);
		for (MigrationInfo info : infoService.all()) {
			assertThat(info.getVersion().getVersion())
					.isIn("1.0.0", "2.0.0", "3.0", "3.0.1", "1.1.1", "1.1.2");
			if (info.getVersion().equals("1.1.2")) {
				assertThat(info.getDescription()).isEqualTo("Late arrival2");
				assertThat(info.getType().name()).isEqualTo(MigrationType.CQL.name());
				assertThat(info.getScript()).contains(".cql");
				assertThat(info.getState().isApplied()).isTrue();
				assertThat(info.getInstalledOn()).isNotNull();
			}
		}

		// test out of order when out of order is allowed again
		String[] outOfOrder3ScriptsLocations = { "migration/integ_outoforder3", "migration/integ/java" };
		cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(outOfOrder3ScriptsLocations);
		cm.getConfigs().setAllowOutOfOrder(true);
		cm.setKeyspace(getKeyspace());
		cm.migrate();

		infoService = cm.info();
		System.out.println("Out of order migration with out-of-order allowed");
		System.out.println(MigrationInfoDumper.dumpToAsciiTable(infoService.all()));
		assertThat(infoService.all()).hasSize(7);
		for (MigrationInfo info : infoService.all()) {
			assertThat(info.getVersion().getVersion())
					.isIn("1.0.0", "2.0.0", "3.0", "3.0.1", "1.1.1", "1.1.2", "1.1.3");
			if (info.getVersion().equals("1.1.3")) {
				assertThat(info.getDescription()).isEqualTo("Late arrival3");
				assertThat(info.getType().name()).isEqualTo(MigrationType.CQL.name());
				assertThat(info.getScript()).contains(".cql");
				assertThat(info.getState().isApplied()).isTrue();
				assertThat(info.getInstalledOn()).isNotNull();
			}
		}
	}

	@Test
	public void testValidate() {
		// apply migration scripts
		String[] scriptsLocations = { "migration/integ", "migration/integ/java" };
		CassandraMigration cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(scriptsLocations);
		cm.setKeyspace(getKeyspace());
		cm.migrate();

		MigrationInfoService infoService = cm.info();
		String validationError = infoService.validate();
		assertThat(validationError).isNull();

		cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(scriptsLocations);
		cm.setKeyspace(getKeyspace());
		cm.validate();

		cm = new CassandraMigration();
		cm.getConfigs().setScriptsLocations(new String[] { "migration/integ/java" });
		cm.setKeyspace(getKeyspace());
		try {
			cm.validate();
			fail("The expected CassandraMigrationException was not raised");
		} catch (CassandraMigrationException e) {
		}
	}

	static boolean runCmdTestCompleted = false;
	static boolean runCmdTestSuccess = false;

	@Test
	public void runCmdTest() throws IOException, InterruptedException {
		String shell = "java -jar"
				+ " -Dcassandra.migration.scripts.locations=filesystem:target/test-classes/migration/integ"
				+ " -Dcassandra.migration.cluster.contactpoints=" + BaseIT.getContactPoint()
				+ " -Dcassandra.migration.cluster.port=" + BaseIT.getPort()
				+ " -Dcassandra.migration.cluster.username=" + BaseIT.getUsername()
				+ " -Dcassandra.migration.cluster.password=" + BaseIT.getPassword()
				+ " -Dcassandra.migration.keyspace.name=" + BaseIT.CASSANDRA__KEYSPACE
				+ " target/*-jar-with-dependencies.jar" + " migrate";
		ProcessBuilder builder;
		if (isWindows()) {
			throw new IllegalStateException();
		} else {
			builder = new ProcessBuilder("bash", "-c", shell);
		}
		builder.redirectErrorStream(true);
		final Process process = builder.start();

		watch(process);

		while (!runCmdTestCompleted)
			Thread.sleep(1000L);

		assertThat(runCmdTestSuccess).isTrue();
	}

	private static void watch(final Process process) {
		new Thread(new Runnable() {
			public void run() {
				BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				try {
					while ((line = input.readLine()) != null) {
						if (line.contains("Successfully applied 2 migrations"))
							runCmdTestSuccess = true;
						System.out.println(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				runCmdTestCompleted = true;
			}
		}).start();
	}

	private boolean isWindows() {
		return (System.getProperty("os.name").toLowerCase()).contains("win");
	}
}
