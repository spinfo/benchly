package benchly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Config {

	private static final Logger LOG = LoggerFactory.getLogger(Config.class);

	private static final Options OPTIONS = new Options();
	static {
		OPTIONS.addOption("d", "jdbc-url", true, "The jdbc connection that should be used (as a jdbc url).");
		OPTIONS.addOption("f", "frontend-path", true, "The location to serve the frontend from (if wished for).");
		OPTIONS.addOption("h", "help", false, "Display this help.");
	}

	// the database url to use
	private String jdbcUrl = null;

	// whether the frontend should be served by this application and where to serve
	// it from
	private boolean shouldServeTheFrontend = false;
	private String frontendPath = null;

	private Config() {
	}

	protected static Config from(String[] args) {
		CommandLineParser parser = new DefaultParser();

		Config config = new Config();

		try {
			CommandLine cl = parser.parse(OPTIONS, args);

			// if the help option is present, show the help and exit
			if (cl.hasOption("help")) {
				(new HelpFormatter()).printHelp("java -jar modulewebserver.jar", OPTIONS);
				System.exit(0);
			}

			// further options are only read here validity is checked separately
			if (cl.hasOption("jdbc-url")) {
				config.jdbcUrl = cl.getOptionValue("jdbc-url");
			}
			if (cl.hasOption("frontend-path")) {
				config.shouldServeTheFrontend = true;
				config.frontendPath = cl.getOptionValue("frontend-path");
			}
		} catch (ParseException e) {
			LOG.error("Error while parsing the server configuration: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		return config;
	}

	protected String getJdbcUrl() {
		return jdbcUrl;
	}

	protected boolean frontendWasRequested() {
		return shouldServeTheFrontend;
	}

	protected String getFrontendPath() {
		return frontendPath;
	}

	protected List<String> checkForErrors() {
		List<String> errors = new ArrayList<>();

		// the jdbc url is required
		if (StringUtils.isBlank(jdbcUrl)) {
			errors.add("No jdbc url given on command line. Shutting down.");
		}

		// frontend location is not required
		if (shouldServeTheFrontend) {
			// check argument exists
			if (StringUtils.isBlank(frontendPath)) {
				errors.add("Option for frontend path is given blank without an argument value.");
			} else {
				// check that the argument is a valid directory
				File file = new File(frontendPath);
				if (!file.isDirectory() || !file.canRead() || !file.canExecute()) {
					errors.add("Frontend path is not a directory we have read and execute right for.");
				}
			}
		}

		return errors;
	}

}
