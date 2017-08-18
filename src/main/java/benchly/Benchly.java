package benchly;

import static spark.Spark.*;

public class Benchly {

	public static void main(String[] args) {
		get("hello", (request, response) -> "Hello World :)");
	}
}
