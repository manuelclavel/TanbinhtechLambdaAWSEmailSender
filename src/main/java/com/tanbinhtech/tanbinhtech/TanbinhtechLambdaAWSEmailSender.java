package com.tanbinhtech.tanbinhtech;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;

import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;
import software.amazon.awssdk.services.sesv2.SesV2Client;

public class TanbinhtechLambdaAWSEmailSender implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static Integer sendEmail(JSONObject json, LambdaLogger logger) {
        //throws UnsupportedEncodingException {

        try (SesV2Client client = SesV2Client.builder()
                .region(Region.AP_SOUTHEAST_1) // Ensure region matches where identity is verified
                .build()) {

            Destination destination = Destination.builder()
                    .toAddresses(json.getString("receiver"))
                    .build();

            Content content = Content.builder()
                    .data(json.getString("body"))
                    .charset("UTF-8")
                    .build();

            //Content htmlContent = Content.builder()
            //        .data(BODY_HTML)
            //        .charset("UTF-8")
            //        .build();
            Body body = Body.builder()
                    .text(content)
                    //.html(htmlContent)
                    .build();

            Message message = Message.builder()
                    .subject(Content.builder().data(json.getString("subject"))
                            .charset("UTF-8").build())
                    .body(body)
                    .build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(destination)
                    .content(EmailContent.builder().simple(message).build())
                    .fromEmailAddress(json.getString("sender"))
                    .build();

            client.sendEmail(request);
            //System.out.println("Email sent successfully!");
            return 200;

        } catch (SesV2Exception e) {
            //System.err.println(e.awsErrorDetails().errorMessage());
            //throw new RuntimeException(e);
            logger.log("Email exception: " + e);
            return 500;
        }

    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        String requestBody = request.getBody();

        if (requestBody.equals("warmup-call")) {
            logger.log("Warm-up call");
            APIGatewayProxyResponseEvent response
                    = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody("Warmup-call received.");
            response.setHeaders(java.util.Collections
                    .singletonMap("Content-Type", "text/plain"));
            return response;
        } else {
            JSONObject bodyJSON = new JSONObject(requestBody);

            String messageToUser = bodyJSON.getString("messageToUser");
            String messageFromIdentity = bodyJSON.getString("messageFromIdentity");
            String messageFromApp = bodyJSON.getString("messageFromApp");
            String messageSubject = bodyJSON.getString("messageSubject");
            String messageBody = bodyJSON.getString("messageBody");

            JSONObject message = new JSONObject();
            message.put("sender", messageFromIdentity);
            message.put("nameSender", messageFromApp);
            message.put("subject", messageSubject);
            message.put("receiver", messageToUser);
            message.put("body", messageBody);

            Integer codeSendEmail = sendEmail(message, logger);
            String messageSendEmail;
            if (codeSendEmail == 200) {
                messageSendEmail = "Email successfully sent";
            } else {
                messageSendEmail = "Error when sending the email";
            }

            Map<String, String> headersMap;
            headersMap = Map.of(
                    "content-type", "application/json");

            JSONObject bodyResponse = new JSONObject();
            bodyResponse.put("code", codeSendEmail);
            bodyResponse.put("message", messageSendEmail);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(codeSendEmail)
                    .withHeaders(headersMap)
                    .withBody(bodyResponse.toString())
                    .withIsBase64Encoded(true);

        }

    }

}
