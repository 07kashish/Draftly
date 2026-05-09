package com.draftly.service;

import com.draftly.config.OpenRouterConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterLlmService implements LlmService {

    private static final String SYSTEM_PROMPT = """
            You are Draftly, an AI Gmail reply assistant. Generate only the final email reply body.
            Do not include explanations, analysis, extracted metadata, markdown, subject lines, labels, or extra notes.

            Important roles:
            - userName is the person sending the reply.
            - sender is the person receiving the reply.
            - Do not greet userName.
            - Greet the sender when a greeting is appropriate.
            - If sender is an email like client@example.com, use a natural greeting such as "Hi Client" or "Hi there".
            - Sign off using userName only if a closing is needed.

            Intent rules:
            - Before writing the reply, consider:
              1. What is the sender asking?
              2. Are there multiple questions?
              3. What action does the sender need from the user?
              4. What information is unknown and should not be invented?
            - Identify the sender's main intent before writing.
            - Answer every question/request in the email.
            - If the email asks a question, answer that question directly.
            - If the email asks "when are you ready", address readiness politely.
            - If the email asks for a meeting, address meeting scheduling clearly.
            - If actual availability is unknown, ask the sender to share available slots or say the user will confirm a suitable time shortly.
            - If the email asks for a decision and the user's decision is not provided, do not invent a decision.
            - For unknown decisions, say the user is reviewing it and will confirm soon, or ask for a short extension.
            - Never respond with only "noted" when the sender is asking for a decision, confirmation, availability, payment, acceptance, or action.
            - Do not generate vague replies like "Thank you for reaching out" only.
            - Do not invent exact times, dates, decisions, payments, or commitments.
            - For informational emails, a simple acknowledgement is acceptable.

            Name rules:
            - Use the sender's name cleanly.
            - If the extracted sender name looks like an email username such as "Elixirjain", convert it to a clean name if possible, like "Elixir".
            - Do not produce awkward greetings like "Hi Elixirjain,".
            - Use the sender display name for the greeting when available.
            - Do not derive the greeting from the email address if sender display name is available.

            Be natural, professional, concise, and specific to the email. Do not invent facts. Use the user's name/signature from the request.

            Final output rule:
            - Return only the email that should be inserted into Gmail.
            - Start directly with the greeting, such as "Hi Name," or "Dear Team,".
            - Never include lines like "Sender's main intent", "Questions to answer", "Reply strategy", "User name", or "clean name".
            """;

    private static final String COMPOSE_SYSTEM_PROMPT = """
            You are an expert email writing assistant.
            Generate a complete, polished email from the user's instruction.

            Output exactly in this format:
            Subject: <clear subject line>

            Body:
            <complete email body>

            Return a professional email with:
            - clear subject
            - appropriate greeting
            - specific body based on the prompt
            - polite closing
            - user's name in signature

            Rules:
            - Follow requested tone and length.
            - Use provided context if available.
            - Do not invent facts, deadlines, payments, acceptances, dates, or commitments unless explicitly stated.
            - Do not write vague one-line bodies.
            - Do not use broken grammar.
            - If the prompt is short, infer a reasonable professional structure.
            - If the prompt mentions deadline extension, clearly explain that the user is requesting more time.
            - Keep it concise but complete.
            """;

    private final OpenRouterConfig openRouterConfig;
    private final WebClient.Builder webClientBuilder;

    @Override
    public String generateReply(
            String userName,
            String sender,
            String senderName,
            String senderEmail,
            String subject,
            String body,
            String threadHistory,
            String category,
            String tone,
            String urgency,
            String strategy,
            String userToneProfileContext
    ) {
        if (!StringUtils.hasText(openRouterConfig.getApiKey())) {
            throw new RuntimeException("OPENROUTER_API_KEY is missing or blank");
        }

        log.info("Generating draft using OpenRouter model: {}", openRouterConfig.getModel());

        OpenRouterRequest request = new OpenRouterRequest(
                openRouterConfig.getModel(),
                List.of(
                        new Message("system", SYSTEM_PROMPT),
                        new Message("user", buildUserPrompt(
                                userName,
                                sender,
                                senderName,
                                senderEmail,
                                subject,
                                body,
                                threadHistory,
                                category,
                                tone,
                                urgency,
                                strategy,
                                userToneProfileContext
                        ))
                ),
                0.4,
                350
        );

        OpenRouterResponse response = webClientBuilder.build()
                .post()
                .uri(openRouterConfig.getBaseUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterConfig.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "Draftly")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("OpenRouter returned no choices");
        }

        Message message = response.choices().get(0).message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new RuntimeException("OpenRouter returned an empty reply");
        }

        return message.content().trim();
    }

    @Override
    public String generateComposeEmail(
            String userName,
            String recipient,
            String prompt,
            String tone,
            String context,
            String desiredLength,
            String userToneProfileContext
    ) {
        if (!StringUtils.hasText(openRouterConfig.getApiKey())) {
            throw new RuntimeException("OPENROUTER_API_KEY is missing or blank");
        }

        log.info("Generating compose email using OpenRouter model: {}", openRouterConfig.getModel());

        OpenRouterRequest request = new OpenRouterRequest(
                openRouterConfig.getModel(),
                List.of(
                        new Message("system", COMPOSE_SYSTEM_PROMPT),
                        new Message("user", buildComposePrompt(
                                userName,
                                recipient,
                                prompt,
                                tone,
                                context,
                                desiredLength,
                                userToneProfileContext
                        ))
                ),
                0.4,
                500
        );

        OpenRouterResponse response = webClientBuilder.build()
                .post()
                .uri(openRouterConfig.getBaseUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterConfig.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "Draftly")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("OpenRouter returned no choices");
        }

        Message message = response.choices().get(0).message();
        if (message == null || !StringUtils.hasText(message.content())) {
            throw new RuntimeException("OpenRouter returned an empty compose email");
        }

        return message.content().trim();
    }

    private String buildUserPrompt(
            String userName,
            String sender,
            String senderName,
            String senderEmail,
            String subject,
            String body,
            String threadHistory,
            String category,
            String tone,
            String urgency,
            String strategy,
            String userToneProfileContext
    ) {
        return """
                User name: %s
                Sender: %s
                Sender display name: %s
                Sender email: %s
                Subject: %s
                Email body: %s
                Thread history: %s
                Detected category: %s
                Detected tone: %s
                Detected urgency: %s
                Reply strategy: %s
                %s

                Remember: the reply is from userName to sender. Do not greet userName. Greet sender instead.
                Use the sender display name for the greeting when available. Do not derive the greeting from the email address if sender display name is available.
                Write the final email reply now. Output only the email body, starting with the greeting.
                """.formatted(
                safe(userName),
                safe(sender),
                safe(senderName),
                safe(senderEmail),
                safe(subject),
                safe(body),
                safe(threadHistory),
                safe(category),
                safe(tone),
                safe(urgency),
                safe(strategy),
                safe(userToneProfileContext)
        );
    }

    private String buildComposePrompt(
            String userName,
            String recipient,
            String prompt,
            String tone,
            String context,
            String desiredLength,
            String userToneProfileContext
    ) {
        return """
                User name: %s
                Recipient: %s
                User prompt: %s
                Requested tone: %s
                Optional context: %s
                Desired length: %s
                %s

                Generate the subject and complete email body now.
                """.formatted(
                safe(userName),
                safe(recipient),
                safe(prompt),
                safe(tone),
                safe(context),
                safe(desiredLength),
                safe(userToneProfileContext)
        );
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "";
    }

    private record OpenRouterRequest(
            String model,
            List<Message> messages,
            double temperature,
            int max_tokens
    ) {
    }

    private record Message(String role, String content) {
    }

    private record OpenRouterResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
