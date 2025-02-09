package org.shimopino.lambda;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/*
 * このクラスは、CDI (Context and Dependency Injection) を利用して OpenTelemetry の Tracer を
 * アプリケーション全体で利用できるように生成・提供するための設定クラスです。
 *
 * ◆ 背景および理由 ◆
 *
 * 1. 環境ごとのTracerの設定
 *   - AWS Lambda 環境では、ADOT Lambda Layer が自動的にTracerを設定するため、Lambda実行中には
 *     独自のTracer作成は不要です。そのため、環境変数 "AWS_LAMBDA_FUNCTION_NAME" により判定し、
 *     Lambda環境の場合はTracerProvider.noop() を利用し、何もトレースを行わない（または最小限の）
 *     状態にしています。
 *   - 一方で、ローカル開発や非Lambda環境では、SdkTracerProvider.builder() を使用して実際に
 *     Tracer を構築し、トレースデータが適切に取得できるようにしています。
 *
 * 2. CDI の利用 (@ApplicationScoped と @Produces)
 *   - @ApplicationScoped アノテーションは、クラスやメソッドで生成されるオブジェクトのライフサイクルを
 *     アプリケーション全体にわたって共有するために利用しています。これにより、Tracerはシングルトン
 *     のように扱われ、複数回初期化されることなく再利用可能となります。
 *   - @Produces アノテーションは、このクラスが生成する Tracer インスタンスを CDI コンテナに
 *     登録するために用います。これにより、他のコンポーネントで @Inject を用いて簡単にTracerを参照でき、
 *     依存性注入により管理が容易になります。
 *
 * 3. Tracerの名称指定 ("otel-lambda-handler")
 *   - TracerProvider.get() メソッドで指定する名前は、トレースデータのログや分析時にどのサービスから
 *     発行されたものかを識別するために利用されます。ここでは "otel-lambda-handler" としているため、
 *     統一された名称でサービスのトレース情報が記録され、後からの解析やデバッグが容易になります。
 */

@ApplicationScoped
public class TracerConfig {

    // このメソッドによって、CDIコンテナが管理するTracerのBeanが生成されます。
    // @Producesにより他のコンポーネントで@Injectした際に利用可能となります。
    @Produces
    @ApplicationScoped
    public Tracer produceTracer() {
        // AWS Lambda環境かどうかを環境変数で判定します。
        // Lambda環境ではADOT Lambda Layerが自動でTracerを設定しているため、独自生成は必要ありません。
        // そのため、初期値としてTracerProvider.noop()（何もトレースをしないプロバイダー）を設定します。
        TracerProvider tracerProvider = TracerProvider.noop();
        
        // ローカル開発やその他の環境の場合、
        // 実際にトレースを有効にするためにSdkTracerProviderを利用してTracerを生成します。
        if (!isLambdaEnvironment()) {
            tracerProvider = SdkTracerProvider.builder().build();
        }
        
        // Tracerのインスタンスを取得する際に "otel-lambda-handler" という名前を付与します。
        // この名称によりログやトレースデータ上で、どのサービスのトレースなのかを一目で識別できるようにします。
        return tracerProvider.get("otel-lambda-handler");
    }

    private boolean isLambdaEnvironment() {
        return System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null;
    }
}