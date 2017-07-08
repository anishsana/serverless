package serverless;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class LambdaGradebook implements RequestHandler<S3Event, String> {

	private AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
	private AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
	private DynamoDB dynamoDB = new DynamoDB(client);

	private static String tableName = "Students";

	public LambdaGradebook() {
	}

	// Test purpose only.
	LambdaGradebook(AmazonS3 s3) {
		this.s3 = s3;
	}

	public String handleRequest(S3Event event, Context context) {
		context.getLogger().log("Received event: " + event);

		// Get the object from the event and show its content type
		String bucket = event.getRecords().get(0).getS3().getBucket().getName();
		String key = event.getRecords().get(0).getS3().getObject().getKey();
		try {
			S3Object response = s3.getObject(new GetObjectRequest(bucket, key));
			String contentType = response.getObjectMetadata().getContentType();

			BufferedReader br = new BufferedReader(new InputStreamReader(response.getObjectContent()));
			// Calculate grade
			String csvOutput;
			while ((csvOutput = br.readLine()) != null) {
				String[] str = csvOutput.split(",");
				int total = 0;
				int average = 0;
				for (int i = 1; i < str.length; i++) {
					total += Integer.valueOf(str[i]);
				}
				average = total / (str.length - 1);
				createDynamoItem(Integer.valueOf(str[0]), average);
			}
			return contentType;
		} catch (IOException e) {
			e.printStackTrace();
			context.getLogger().log(String.format("Error getting object %s from bucket %s. Make sure they exist and"
					+ " your bucket is in the same region as this function.", bucket, key));
			return e.toString();
		}

	}

	private void createDynamoItem(int studentId, int grade) {

		Table table = dynamoDB.getTable(tableName);
		try {

			Item item = new Item().withPrimaryKey("StudentID", studentId).withInt("Grade", grade);
			table.putItem(item);

		} catch (Exception e) {
			System.err.println("Create item failed.");
			System.err.println(e.getMessage());

		}
	}
}