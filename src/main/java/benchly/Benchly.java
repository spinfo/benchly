package benchly;

import static spark.Spark.get;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import base.workbench.ModuleWorkbenchController;
import modules.Module;

public class Benchly {

	private static final Logger LOGGER = LoggerFactory.getLogger(Benchly.class);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static void main(String[] args) {

		get("modules", "application/json", (request, response) -> {

			ModuleWorkbenchController controller = new ModuleWorkbenchController();
			Map<String, Module> modules = controller.getAvailableModules();

			List<ModuleProfile> profiles = modules.values().stream().map((Module m) -> new ModuleProfile(m))
					.collect(Collectors.toList());

			response.type("application/json");
			if (profiles == null || profiles.isEmpty()) {
				LOGGER.error("No modules could be loaded.");
				return null;
			} else {
				return profiles;
			}
		}, GSON::toJson);

	}

}
