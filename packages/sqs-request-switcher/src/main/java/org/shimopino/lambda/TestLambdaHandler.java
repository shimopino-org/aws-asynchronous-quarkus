package org.shimopino.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * AWS Lambda用のハンドラークラス
 * 
 * 注意点：
 * 1. @ApplicationScopedアノテーションが必須
 * - QuarkusのCDIコンテナでLambdaハンドラーを管理するために必要
 * - これがないと「Unable to find handler class」エラーが発生
 * 
 * 2. RequestHandler<Integer, Integer>の実装
 * - AWSのLambdaハンドラーインターフェースを実装する必要がある
 * - RequestHandlerまたはRequestStreamHandlerのいずれかを実装
 */
@ApplicationScoped
public class TestLambdaHandler implements RequestHandler<Integer, Integer> {

  @Override
  public Integer handleRequest(Integer input, Context context) {
    try {
      Log.info("Processing request with input: " + input);
      Log.info("FunctionName: " + context.getFunctionName());
      Log.info("AwsRequestId: " + context.getAwsRequestId());
      Log.info("FunctionVersion: " + context.getFunctionVersion());
      return 100;
    } catch (Exception exception) {
      Log.error("Error processing request", exception);
      return -1;
    }
  }
}