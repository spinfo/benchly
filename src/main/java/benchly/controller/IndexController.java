package benchly.controller;

import benchly.util.Views;
import spark.Request;
import spark.Response;
import spark.Route;

public class IndexController extends Controller {

	public static Route serveIndexPage = (Request request, Response response) -> {
		return Views.render(request, Views.newContext(), "templates/index.vm");
	};

}
