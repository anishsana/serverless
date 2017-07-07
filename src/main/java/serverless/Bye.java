package serverless;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Bye implements RequestHandler<String, String> {

	public String handleRequest(String input, Context context) {
		String output = "Bye " + input + "!";
		return output;
	}

}
