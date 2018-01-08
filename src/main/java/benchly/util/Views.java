package benchly.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.app.VelocityEngine;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.velocity.VelocityTemplateEngine;

public class Views {
	
	public static final Object EMPTY_REPSONSE = "";

	public static String render(Request request, Map<String, Object> viewContext, String templatePath) {
		// some defaults are included in every model
		viewContext.put("WebPath", Path.Web.class);
		viewContext.put("userMessages", SessionUtil.clearUserMessages(request));
		viewContext.put("user", SessionUtil.getCurrentUser(request));
		viewContext.put("currentPath", request.pathInfo());

		return templateEngine().render(new ModelAndView(viewContext, templatePath));
	}

    public static Route notFound = (Request request, Response response) -> {
        Map<String, Object> model = newContextWith("request", request);
        return render(request, model, Path.Template.STATUS_404);
    };
    
    public static Route errorPage = (Request request, Response response) -> {
    	return render(request, newContext(), Path.Template.STATUS_500);
    };

	private static VelocityTemplateEngine templateEngine() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty("runtime.references.strict", true);
		engine.setProperty("resource.loader", "class");
		engine.setProperty("class.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		return new VelocityTemplateEngine(engine);
	}
	
	public static Map<String, Object> newContext() {
		return new HashMap<>(6);
	}
	
	public static Map<String, Object> newContextWith(String key, Object value) {
		Map<String, Object> model = newContext();
		model.put(key, value);
		return model;
	}
}
