package org.shimopino.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OtelLambdaHandler implements RequestHandler<SQSEvent, Void> {
    private static final Logger LOG = Logger.getLogger(OtelLambdaHandler.class);

    @Inject
    Tracer tracer;

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        Span parentSpan = tracer.spanBuilder("ProcessSQSMessage")
                .setAttribute("aws.requestId", context.getAwsRequestId())
                .setAttribute("lambda.name", context.getFunctionName())
                .startSpan();

        try (var scope = parentSpan.makeCurrent()) {
            for (SQSEvent.SQSMessage message : event.getRecords()) {
                Span messageSpan = tracer.spanBuilder("ProcessIndividualMessage")
                        .setAttribute("message.id", message.getMessageId())
                        .startSpan();
                
                try {
                    LOG.infof("Processing message: %s", message.getBody());
                    // ここにメッセージ処理ロジックを実装
                } catch (Exception e) {
                    messageSpan.recordException(e);
                    throw e;
                } finally {
                    messageSpan.end();
                }
            }
            return null;
        } finally {
            parentSpan.end();
        }
    }
}