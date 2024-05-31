package computerdatabase;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.sse;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * First I want to have a feeder to send some prompts (i should go for a CSV file)
 * I should connect to the api of ChatGPT using POST SSE,
 * check the return and if all NONE then close the SSE
 * With their curl request typical message
 * data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"role":"assistant","content":""},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":"Hello"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":"!"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" How"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" can"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" I"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" assist"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" you"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":" today"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{"content":"?"},"logprobs":null,"finish_reason":null}]}

data: {"id":"chatcmpl-9UVopmpnbW8qELkOfFG1ofE04h0ZP","object":"chat.completion.chunk","created":1717059179,"model":"gpt-3.5-turbo-0125","system_fingerprint":null,"choices":[{"index":0,"delta":{},"logprobs":null,"finish_reason":"stop"}]}

data: [DONE]

curl https://api.openai.com/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-3.5-turbo",
    "stream": true,  
    "messages": [
      {
        "role": "system",
        "content": "You are a helpful assistant."
      },
      {
        "role": "user",
        "content": "Hello!"
      }
    ]
    }
 */




public class ComputerDatabaseSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol =
    http.baseUrl("https://api.openai.com/v1/chat");

    ScenarioBuilder prompt = scenario("Scenario").exec(

        sse("Connect")
        .post("/completions")
        .header("Authorization", "Bearer <token>")
        .body(StringBody("{\"model\": \"gpt-3.5-turbo\",\"stream\":true,\"messages\":[{\"role\":\"system\",\"content\":\"You are a helpful assistant.\"},{\"role\":\"user\",\"content\":\"Just say HI\"}]}"))
        .asJson()
        ).pause(1)
        .exec(
          sse("Wait for message").setCheck().await(300).on(sse.checkMessage("checkCustom").check(regex(".*").saveAs("response")))
        ).asLongAs(session -> !session.get("response").toString().contains("stop")).on(
          exec(session -> {      
            System.out.println(session.getString("response"));
            return session;})
          .exec(
            sse("Wait for next message").setCheck().await(300).on(sse.checkMessage("checkCustom").check(regex(".*").saveAs("response")))
            )
            
        ).exec(sse("checkCustom").close());

    {
        setUp(
          prompt.injectOpen(constantUsersPerSec(1).during(15))
        ).protocols(httpProtocol);
    }
}
